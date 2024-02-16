package ibb.api.geneservice.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import ibb.api.geneservice.parser.FastaParser;
import ibb.api.geneservice.parser.FastaRecord;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SequenceIndex {
    @Inject
    ElasticsearchClient esClient;
    
    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    String indexPrefix;

    public void loadFromFasta(String species, String type, Path path) throws IOException {
        try (
            Stream<FastaRecord> sequences = FastaParser.parse(path);
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {

            Log.infov("Loading {0}(s) for species {1} from {2}", type, species, path.toString());
            String indexName = getIndexName(species, type);
            AtomicInteger counter = new AtomicInteger(0);
            sequences.forEach(sequence -> {
                counter.incrementAndGet();
                ingester.add(op -> op
                    .index(idx -> idx
                        .index(indexName)
                        .id(sequence.header)
                        .document(sequence)
                    ));
            });
            Log.infov("Loaded {0} {1}(s) for species {2}", counter.get(), type, species);
        }
    }

    private String getIndexName(String species, String type) {
        return indexPrefix + "-" + species.toLowerCase() + "-" + type + "-sequences";
    }

    public boolean exists(String species, String type) throws IOException {
        return esClient.indices().exists(i -> i.index(getIndexName(species, type))).value();
    }

    public void createIndex(String species, String type) throws IOException {
        String indexName = getIndexName(species, type);
        esClient.indices().create(i -> i
            .index(indexName)
            .mappings(m -> m
                .properties("sequence", p -> p
                    .text(t -> t.index(false))
                )
            ));
    }

    public void deleteIndexIfExists(String species, String type) throws IOException {
        String indexName = getIndexName(species, type);
        esClient.indices().delete(i -> i.index(indexName).ignoreUnavailable(true));
    }
}
