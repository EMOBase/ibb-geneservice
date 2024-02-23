package ibb.api.geneservice.es;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.parser.TextParserException;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public abstract class ESSourceIndex<T> {

    @Inject
    ESHelper esHelper;

    @Inject
    ElasticsearchClient esClient;

    /**
     * The key to build names of aliases and indices for this source.
     */
    private String key;
    private String alias;

    public ESSourceIndex(String key) {
        this.key = key;
    }

    @PostConstruct
    public void setup() {
        alias = esHelper.getESName(key);

        if (shouldDeleteOnStart()) {
            delete();
        }
        try {
			esClient.indices().putIndexTemplate(it -> it
                .name(alias + "-template")
                .indexPatterns(alias + "-*")
                .template(t -> t
                    .mappings(getTypeMapping())
                    .aliases(alias,  a -> a)
                )
            );
		} catch (IOException e) {
            throw new UncheckedIOException(e);
		}
    }

    public String getQueryIndexName() {
        return alias;
    }

    public boolean exists() {
        try {
            return esClient.indices().existsAlias(a -> a.name(alias)).value();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void refresh() {
        try {
            esClient.indices().refresh(r -> r.index(alias).ignoreUnavailable(true));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void delete() {
        try {
            List<String> indices = esClient.indices().getAlias(a -> a.name(alias)).result()
                .keySet().stream().toList();
            esClient.indices().delete(d -> d.index(indices));
        } catch (ElasticsearchException e) {
            if (e.status() != 404) {
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Load the given source into elasticsearch. Also create aliases.
     * 
     * @param source the source to load
     * @return the number of items loaded
     * @throws IOException if an error occurs while loading
     */
    public long load(DocumentSource<T> source) {
        try {
            return _load(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long _load(DocumentSource<T> source) throws IOException {
        String indexName = esHelper.getESName(key, source.tags) + "-" + esHelper.getTimestamp();
        AtomicLong counter = new AtomicLong(0);

        try (
            Stream<T> items = source.stream();
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient));
        ) {
            Log.infov("Loading {0} {1} items from {2}", alias, source.tags, source.file.toPath().toString());
            items.forEach(item -> {
                counter.incrementAndGet();
                ingester.add(op -> op
                    .index(idx -> idx
                        .pipeline(source.ingestPipeline)
                        .index(indexName)
                        .document(item)
                    ));
            });
        } catch (TextParserException e) {
            esClient.indices().delete(d -> d.index(indexName).ignoreUnavailable(true));
            throw e;
        }
        Log.infov("Loaded {0} {1} items", counter.get(), alias);
        return counter.get();
    }

    protected ElasticsearchClient getESClient() {
        return esClient;
    }
    protected ESHelper getESHelper() {
        return esHelper;
    }

    protected abstract boolean shouldDeleteOnStart();
    protected abstract TypeMapping getTypeMapping();
}
