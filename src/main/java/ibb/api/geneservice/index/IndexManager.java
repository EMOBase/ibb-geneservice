package ibb.api.geneservice.index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import ibb.api.geneservice.parser.TextParserException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IndexManager {
    
    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    String indexPrefix;


    /**
     * Load documents from multiple sources into an index only if the index does not exist.
     * 
     * @param <T> the type of the documents
     * @param prefix a unique prefix to construct the index name and alias
     * @param sources a list of document sources
     */
    public <T> void loadAllIfNotExists(String prefix, List<DocumentSource<T>> sources) {
        try {
            if (!exists(prefix)) {
                for (var source : sources) {
                    load(prefix, source);
                };
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Load documents from a source into an index. This method will create an index
     * with the name {@code prefix-timestamp}, which can be queried through the alias
     * {@code prefix}.
     * 
     * Previous indices with the same alias will be deleted.
     * The use of the alias makes sure documents are inserted in a transaction manner.
     * 
     * @param <T> the type of the documents
     * @param prefix a unique prefix to construct the index name and alias
     * @param source a document source
     * @throws IOException if an I/O error occurs
     */
    public <T> void load(String prefix, DocumentSource<T> source) throws IOException {
        String aliasName = getAliasName(prefix);
        String indexName = aliasName + "-" + getTimestamp();
        AtomicInteger counter = new AtomicInteger(0);

        try (
            Stream<T> items = source.parser.parse(source.file.toPath());
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {
            Log.infov("Loading {0} items from {1}", aliasName, source.file.toPath().toString());
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
        Log.infov("Loaded {0} {1} items", counter.get(), aliasName);
    }

    public boolean exists(String prefix) throws IOException {
        return esClient.indices().existsAlias(a -> a.name(getAliasName(prefix))).value();
    }

    /**
     * Get the name of the alias which can be used to query the index.
     * 
     * @param prefix the prefix used to construct the alias
     * @return the alias name
     */
    public String getAliasName(String prefix) {
        return prefix.toLowerCase().replace(" ", "_");
    }

    public String getPrefix(IndexType type, String... suffices) {
        String prefix = indexPrefix + "-" + type.name();
        if (suffices.length == 0) {
            return prefix;
        } else {
            return prefix + "-" + String.join("-", suffices);
        }
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
