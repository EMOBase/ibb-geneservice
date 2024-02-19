package ibb.api.geneservice.ortholog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrthologIndex {
    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    String indexPrefix;

    public void loadOrthologs(String source, Path path) throws IOException {
        try (
            Stream<Ortholog> orthologs = new OrthologParser(source).parse(path);
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {

            Log.infov("Loading {0} orthologs from {1}", source, path.toString());
            String indexName = getIndexName(source);
            AtomicInteger counter = new AtomicInteger(0);
            orthologs.forEach(ortholog -> {
                counter.incrementAndGet();
                ingester.add(op -> op
                    .index(idx -> idx
                        .index(indexName)
                        .document(ortholog)
                    ));
            });
            Log.infov("Loaded {0} {1} orthologs", counter.get(), source);
        }
    }

    public String getIndexName(String source) {
        return indexPrefix + "-orthologs-" + source.toLowerCase();
    }

    public boolean exists(String species) throws IOException{
        return esClient.indices().exists(i -> i.index(getIndexName(species))).value();
    }

    public void createIndex(String species) throws IOException {
        String indexName = getIndexName(species);
        esClient.indices().create(i -> i.index(indexName));
    }

    public void deleteIndexIfExists(String species) throws IOException {
        String indexName = getIndexName(species);
        esClient.indices().delete(i -> i.index(indexName).ignoreUnavailable(true));
    }
}
