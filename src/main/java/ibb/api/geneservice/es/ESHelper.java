package ibb.api.geneservice.es;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.analysis.Analyzer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ESHelper {
    
    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    Optional<String> indexPrefix;

    @Inject
    ElasticsearchClient esClient;

    public void deleteIndicesByAliasIgnoreUnavailable(String alias) {
        try {
            List<String> indices = getESClient().indices()
                .getAlias(a -> a.name(alias))
                .result().keySet().stream().toList();

            if (!indices.isEmpty()) {
                getESClient().indices().delete(d -> d.index(indices));
            }
        } catch (ElasticsearchException e) {
            if (e.status() != 404) {
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public void deleteIndexTemplateIgnoreUnavailable(String name) {
        try {
            getESClient().indices().deleteIndexTemplate(d -> d.name(name));
        } catch (ElasticsearchException e) {
            if (e.status() != 404) {
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Analyzer getWhitespaceLowercaseAnalyzer() {
        return Analyzer.of(a -> a
            .custom(c -> c
                .tokenizer("whitespace")
                .filter("lowercase")
            )
        );
    }

    public Analyzer getKeywordLowercaseAnalyzer() {
        return Analyzer.of(a -> a
            .custom(c -> c
                .tokenizer("keyword")
                .filter("lowercase")
            )
        );
    }

    public ElasticsearchClient getESClient() {
        return esClient;
    }


    /**
     * Build an elasticsearch name from the given suffices.
     * The result would be in the form of {@code indexPrefix-suffix1-suffix2-...-suffixN}.
     * 
     * @param suffices the suffices to use
     * @return the normalized elasticsearch name
     */
    public String getESName(List<String> suffices) {
        List<String> prefices = indexPrefix.map(p -> List.of(p)).orElse(Collections.emptyList());
        List<String> words = Stream.of(prefices, suffices)
            .flatMap(List::stream)
            .filter(s -> !s.isBlank())
            .toList();
        return String.join("-", words).toLowerCase().replace(" ", "_");
    }

    /**
     * @see #getESName(List)
     */
    public String getESName(String... suffices) {
        return getESName(List.of(suffices));
    }

    /**
     * @see #getESName(List)
     */
    public String getESName(String suffix, List<String> suffices) {
        return getESName(Stream.concat(Stream.of(suffix), suffices.stream()).toList());
    }

    public String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}
