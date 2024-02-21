package ibb.api.geneservice.index;

import java.io.IOException;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ESEnrichProcessor {

    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    String indexPrefix;

    public void createSynonymEnrichPolicy() throws IOException {
        String indexName = indexPrefix + "-synonyms-*";
        String policyName = getSynonymEnrichPolicyName();

        esClient.enrich().putPolicy(p -> p
            .name(policyName)
            .match(m -> m
                .indices(indexName)
                .matchField("gene")
                .enrichFields(List.of("type", "value"))
            ));
    }

    public void executeSynonymEnrichPolicy() throws IOException {
        String policyName = getSynonymEnrichPolicyName();

        esClient.enrich().executePolicy(p -> p
            .name(policyName)
            .waitForCompletion(true)
        );
    }

    public String getSynonymEnrichPolicyName() {
        return indexPrefix + "-policy-synonyms";
    }
}
