package ibb.api.geneservice.es;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import ibb.api.geneservice.parser.TextParserException;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

public abstract class ESSourceIndex<T extends ESDoc> {

    /**
     * The key to build names of aliases and indices for this source.
     */
    private String key;
    private String alias;
    private ESHelper esHelper;
    private Class<T> docType;

    @Inject
    ESDocSourceProvider<T> docSourceProvider;

    public ESSourceIndex(String key, Class<T> docType) {
        this.key = key;
        this.docType = docType;
    }

    
    @Inject
    public void setESHelper(ESHelper esHelper) {
        this.esHelper = esHelper;
        alias = esHelper.getESName(key);
    }

    protected abstract TypeMapping getTypeMapping();
    protected abstract IndexSettingsAnalysis getAnalysis();
    protected abstract boolean shouldDeleteOnStart();

    public void setup(@Observes StartupEvent event) {
        if (shouldDeleteOnStart()) {
            delete();
        }
        try {
			getESClient().indices().putIndexTemplate(it -> it
                .name(alias + "-template")
                .indexPatterns(alias + "-*")
                .template(t -> t
                    .settings(s -> s.analysis(getAnalysis()))
                    .mappings(getTypeMapping())
                    .aliases(alias,  a -> a)
                )
            );
		} catch (IOException e) {
            throw new UncheckedIOException(e);
		}
        if (!exists()) {
            load();
        } else {
            Log.infov("Index {0} already exists", alias);
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
        getESHelper().deleteIndicesByAliasIgnoreUnavailable(alias);
        getESHelper().deleteIndexTemplateIgnoreUnavailable(alias + "-template");
    }

    public SearchResponse<T> search(SearchRequest.Builder builder, int size) {
        try {
            return getESClient().search(builder
                .size(size)
                .index(getQueryIndexName())
                .build()
            , docType);
        } catch (ElasticsearchException e) {
            Log.debug(e.error().causedBy());
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<T> findById(String id) {
        try {
            var response = getESClient().get(g -> g.index(getQueryIndexName()).id(id), docType);
            if (response.found()) {
                return Optional.of(response.source());
            } else {
                return Optional.empty();
            }
        } catch (ElasticsearchException e) {
            Log.debug(e.error().causedBy());
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<T> findByIds(List<String> ids) {
        try {
            var response = getESClient().mget(m -> m
                .index(getQueryIndexName())
                .ids(ids)
            , docType);
            return response.docs().stream()
                .map(d -> {
                    if (d.isResult()) {
                        return d.result().source();
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        } catch (ElasticsearchException e) {
            Log.debug(e.error().causedBy());
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void load() {
        String indexName = esHelper.getESName(key, esHelper.getTimestamp());
        docSourceProvider.provideDocSources().forEach(source -> {
            try {
                load(source, indexName);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void load(ESDocSource<T> source, String indexName) throws IOException {
        try (
            var items = source.stream();
            BulkIngester<String> ingester = BulkIngester.of(b -> b.client(getESClient()))
        ) {
            AtomicLong count = new AtomicLong();
            Log.infov("Loading {0} from {1}", alias, source.file.toPath().toString());
            items.forEach(item -> {
                count.incrementAndGet();
                ingester.add(op -> op
                    .index(idx -> idx
                        .id(item._id())
                        .index(indexName)
                        .document(item)
                    ));
            });
            if (count.get() == 0) {
                Log.warnv("No items found in {0}", source.file.toPath().toString());
            } else {
                Log.infov("Sent {0} items to Elasticsearch", count.get());
            }
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
}
