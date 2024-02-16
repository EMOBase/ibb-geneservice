package ibb.api.geneservice.synonym;

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
public class SynonymIndex {
    
    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    String indexPrefix;

    public void loadSynonyms(String species, Path path, SynonymParser parser) throws IOException {
        try (
            Stream<Synonym> synonyms = parser.parse(path);
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {

            Log.infov("Loading synonyms for species {0} from {1}", species, path.toString());
            String indexName = getIndexName(species);
            AtomicInteger counter = new AtomicInteger(0);
            synonyms.forEach(synonym -> {
                counter.incrementAndGet();
                ingester.add(op -> op
                    .index(idx -> idx
                        .index(indexName)
                        .document(synonym)
                    ));
            });
            Log.infov("Loaded {0} synonyms for species {1}", counter.get(), species);
        }
    }
    
    public String getIndexName(String species) {
        return indexPrefix + "-" + species.toLowerCase() + "-synonyms";
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
