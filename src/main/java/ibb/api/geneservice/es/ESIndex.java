package ibb.api.geneservice.es;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import io.quarkus.logging.Log;
import jakarta.enterprise.inject.spi.CDI;

public class ESIndex<T> {

    private final ElasticsearchClient esClient = CDI.current().select(ElasticsearchClient.class).get();
    private final String indexPrefix = ConfigProvider.getConfig().getValue("geneservice.elasticsearch.index-prefix", String.class);

    private String name;

    public ESIndex(String name) {
        this.name = name;
    }

    public void load(Path path, TextParser<T> parser) throws IOException {
        String aliasName = getAliasName();
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
            List<String> existingIndices = esClient.indices()
                .getAlias(a -> a.name(aliasName))
                .result().keySet().stream().toList();

            esClient.indices().updateAliases(req -> req
                .actions(a -> a.add(r -> r.alias(aliasName).index(indexName)))
                .actions(a -> a.remove(r -> r.alias(aliasName).indices(existingIndices))
            ));
            esClient.indices().delete(d -> d.index(existingIndices));
            Log.infov("Loaded {0} {1} items", counter.get(), name);
        } catch (TextParserException e) {
            esClient.indices().delete(d -> d.index(indexName).ignoreUnavailable(true));
            throw e;
        }
    }

    public String getAliasName() {
        return indexPrefix + "-" + name.toLowerCase();
    }

    public boolean exists() throws IOException {
        return esClient.indices().existsAlias(a -> a.name(getAliasName())).value();
    }

    private static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}
