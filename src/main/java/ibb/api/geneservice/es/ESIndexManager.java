package ibb.api.geneservice.es;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ESIndexManager {
    
    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    String indexPrefix;

    /**
     * Load items from a file into an index. This method will create an index with the name as follows:
     * <pre>
     *    indexPrefix-name-timestamp
     * </pre>
     * which can be queried through the alias:
     * <pre>
     *   indexPrefix-name
     * </pre>
     * Previous indices with the same alias will be deleted.
     * 
     * @param <T> the type of the items
     * @param name a unique name to construct the index name and alias
     * @param path the path to the file
     * @param parser a parser to parse the file into a stream of items
     * @throws IOException if an I/O error occurs
     */
    public <T> void load(String name, Path path, TextParser<T> parser) throws IOException {
        String aliasName = getAliasName(name);
        String indexName = aliasName + "-" + getTimestamp();
        AtomicInteger counter = new AtomicInteger(0);

        try (
            Stream<T> items = parser.parse(path);
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {
            Log.infov("Loading {0} items from {1}", name, path.toString());
            items.forEach(item -> {
                counter.incrementAndGet();
                ingester.add(op -> op
                    .index(idx -> idx
                        .index(indexName)
                        .document(item)
                    ));
            });
        } catch (TextParserException e) {
            esClient.indices().delete(d -> d.index(indexName).ignoreUnavailable(true));
            throw e;
        }

        List<String> existingIndices = getIndicesWithAlias(aliasName);

        esClient.indices().updateAliases(req -> {
            var r = req.actions(a -> a.add(t -> t.alias(aliasName).index(indexName)));
            if (!existingIndices.isEmpty()) {
                r = r.actions(a -> a.remove(t -> t.alias(aliasName).indices(existingIndices)));
            }
            return r;
        });

        if (!existingIndices.isEmpty()) {
            esClient.indices().delete(d -> d.index(existingIndices));
        }
        Log.infov("Loaded {0} {1} items", counter.get(), name);
    }

    public boolean exists(String name) throws IOException {
        return esClient.indices().existsAlias(a -> a.name(getAliasName(name))).value();
    }

    /**
     * Get the name of the alias which can be used to query the index.
     * 
     * @param name the name used to construct the alias
     * @return the alias name
     */
    public String getAliasName(String name) {
        return indexPrefix + "-" + name.toLowerCase();
    }

    private List<String> getIndicesWithAlias(String aliasName) throws IOException {
        try {
            return esClient.indices().getAlias(a -> a.name(aliasName)).result().keySet().stream().toList();
        } catch (ElasticsearchException e) {
            if (e.status() == 404) {
                return List.of();
            } else {
                throw e;
            }
        }
    }

    private static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}
