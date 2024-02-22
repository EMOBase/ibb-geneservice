package ibb.api.geneservice.index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import ibb.api.geneservice.parser.TextParserException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IndexManager {
    
    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    Optional<String> indexPrefix;

    public void deleteAll(IndexType type) throws IOException {
        String name = getIndexName(type) + "*";
        esClient.indices().delete(d -> d.index(name).ignoreUnavailable(true));
    }

    public <T> long loadAllIfNotExists(List<DocumentSource<T>> sources, IndexType type, String ...indexSuffices) {
        long count = 0;
        try {
            String aliasName = getIndexName(type, indexSuffices);
            if (!exists(type, indexSuffices)) {
                for (var source : sources) {
                    count += load(source, type, indexSuffices);
                };
            } else {
                Log.infov("Index {0} already exists", aliasName);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return count;
    }

    private <T> long load(DocumentSource<T> source, IndexType type, String ...indexSuffices) throws IOException {
        String aliasName = getIndexName(type, indexSuffices);
        String indexName = aliasName + "-" + getTimestamp();
        AtomicLong counter = new AtomicLong(0);

        try (
            Stream<T> items = source.stream();
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient));
        ) {
            Log.infov("Loading {0} items from {1}", aliasName, source.file.toPath().toString());
            items.forEach(item -> {
                counter.incrementAndGet();
                ingester.add(op -> op
                    .index(idx -> idx
                        .pipeline(source.ingestPipeline.orElse(null))
                        .index(indexName)
                        .document(item)
                    ));
            });
        } catch (TextParserException e) {
            esClient.indices().delete(d -> d.index(indexName).ignoreUnavailable(true));
            throw e;
        }

        esClient.indices().updateAliases(req -> {
            var r = req.actions(a -> a.add(t -> t.alias(aliasName).index(indexName)));
            if (indexSuffices.length > 0) {
                r = r.actions(a -> a.add(t -> t.alias(getIndexName(type)).index(indexName)));
            }
            return r;
        });
        Log.infov("Loaded {0} {1} items", counter.get(), aliasName);
        return counter.get();
    }

    public String getIndexName(IndexType type, String... suffices) {
        List<String> words = new LinkedList<>();
        words.add(type.name());
        for (String suffix : suffices) {
            words.add(suffix);
        }
        return normalizeName(words.stream().toArray(String[]::new));
    }

    public boolean exists(IndexType type, String... suffices) throws IOException {
        return esClient.indices().existsAlias(a -> a.name(getIndexName(type, suffices))).value();
    }

    public String normalizeName(String ...suffices) {
        List<String> words = new ArrayList<>();
        indexPrefix.ifPresent(p -> words.add(p));
        for (String suffix : suffices) {
            words.add(suffix);
        }
        return String.join("-", words.stream().filter(s -> !s.isBlank()).toList())
            .toLowerCase()
            .replace(" ", "_");
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}
