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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ESHelper {
    
    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    Optional<String> indexPrefix;

    @Inject
    ElasticsearchClient esClient;

    public ElasticsearchClient getESClient() {
        return esClient;
    }

    public boolean enrichPolicyExists(String policyName) {
		try {
			return getESClient().enrich().getPolicy(p -> p.name(policyName)).policies().size() > 0;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

    public void deleteEnrichPolicyIgnoreUnavailable(String policyName) {
		try {
			getESClient().enrich().deletePolicy(p -> p.name(policyName));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (ElasticsearchException e) {
			if (e.status() != 404) {
				throw e;
			}
		}
    }

	public void deletePipelineIgnoreUnavailable(String pipelineName) {
		try {
			getESClient().ingest().deletePipeline(p -> p.id(pipelineName));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (ElasticsearchException e) {
			if (e.status() != 404) {
				throw e;
			}
		}
	}

    public void deleteTransformIgnoreUnavailable(String transformName) {
        try {
            getESClient().transform().deleteTransform(t -> t.transformId(transformName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ElasticsearchException e) {
            if (e.status() != 404) {
                throw e;
            }
        }
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
