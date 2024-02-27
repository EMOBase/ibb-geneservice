package ibb.api.geneservice.es;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import ibb.api.geneservice.parser.TextParserException;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public abstract class ESSourceIndex<T> {

    @Inject
    ESHelper esHelper;

    /**
     * The key to build names of aliases and indices for this source.
     */
    private String key;
    private String alias;

    public ESSourceIndex(String key) {
        this.key = key;
    }
    
    @Inject
    public void setESHelper(ESHelper esHelper) {
        this.esHelper = esHelper;
    }

    @PostConstruct
    protected void init() {
        alias = esHelper.getESName(key);
    }

    public void setup() {
        try {
			getESClient().indices().putIndexTemplate(it -> it
                .name(alias + "-template")
                .indexPatterns(alias + "-*")
                .template(t -> t
                    .settings(s -> s.defaultPipeline(getDefaultPipeline()))
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
            return getESClient().indices().existsAlias(a -> a.name(alias)).value();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void refresh() {
        try {
            getESClient().indices().refresh(r -> r.index(alias).ignoreUnavailable(true));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void delete() {
        try {
            List<String> indices = getESClient().indices().getAlias(a -> a.name(alias)).result()
                .keySet().stream().toList();
            getESClient().indices().delete(d -> d.index(indices));
        } catch (ElasticsearchException e) {
            if (e.status() != 404) {
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try {
            getESClient().indices().deleteIndexTemplate(d -> d.name(alias + "-template"));
        } catch (ElasticsearchException e) {
            if (e.status() != 404) {
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void load(ESDocSource<?> source) {
        try {
            _load(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void _load(ESDocSource<?> source) throws IOException {
        String indexName = esHelper.getESName(key, esHelper.getTimestamp());
        AtomicLong counter = new AtomicLong(0);

        BulkListener<String> listener = new BulkListener<>() {

            @Override
            public void beforeBulk(long executionId, BulkRequest request, List<String> contexts) {
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, List<String> contexts, BulkResponse response) {
                counter.getAndAdd(contexts.size());
                if (response.errors()) {
                    long count = response.items().stream().filter(i -> i.error() != null).count();
                    Log.errorv("Failed to index {0}/{1} {2} items", count, contexts.size(), alias);
                    counter.getAndAdd(-count);
                } else {
                    Log.debugv("Indexed {0} {1} items", contexts.size(), alias);
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, List<String> contexts, Throwable failure) {
                Log.errorv(failure, "Failed to index {0} {1} items", contexts.size(), alias);
            }
        };
        try (
            var items = source.stream();
            BulkIngester<String> ingester = BulkIngester.of(b -> b.client(getESClient()).listener(listener))
        ) {
            Log.infov("Loading {0} from {1}", alias, source.file.toPath().toString());
            items.forEach(item -> {
                ingester.add(op -> op
                    .index(idx -> idx
                        .id(item._id())
                        .index(indexName)
                        .document(item)
                    ), item._id());
            });
        } catch (TextParserException e) {
            getESClient().indices().delete(d -> d.index(indexName).ignoreUnavailable(true));
            throw e;
        }
    }

    protected ElasticsearchClient getESClient() {
        return esHelper.getESClient();
    }

    protected ESHelper getESHelper() {
        return esHelper;
    }

    protected TypeMapping getTypeMapping() {
        return null;
    };

    protected String getDefaultPipeline() {
        return null;
    };
}
