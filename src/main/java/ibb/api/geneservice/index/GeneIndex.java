package ibb.api.geneservice.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import ibb.api.geneservice.model.Gene;
import ibb.api.geneservice.parser.GeneParser;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GeneIndex {
    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    String indexPrefix;

    public void loadFromGFF(String species, Path path) {
        try (
            Stream<Gene> genes = GeneParser.parse(path);
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {

            Log.infov("Loading genes for species {0} from {1}", species, path.toString());
            String indexName = getIndexName(species);
            AtomicInteger counter = new AtomicInteger(0);
            genes.forEach(gene -> {
                counter.incrementAndGet();
                ingester.add(op -> op
                    .index(idx -> idx
                        .index(indexName)
                        .id(gene.id)
                        .document(gene)
                    ));
            });
            Log.infov("Loaded {0} genes for species {1}", counter.get(), species);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists(String species) {
        String indexName = getIndexName(species);
        try {
            return esClient.indices().exists(i -> i.index(indexName)).value();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getIndexName(String species) {
        return indexPrefix + "-" + species.toLowerCase() + "-genes";
    }

    public void createIndex(String species) {
        String indexName = getIndexName(species);
        try {
            esClient.indices().create(i -> i.index(indexName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteIndexIfExists(String species) {
        String indexName = getIndexName(species);
        try {
            esClient.indices().delete(i -> i.index(indexName).ignoreUnavailable(true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
