package ibb.api.geneservice.es;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PipelineManager {

    // public static enum PipelineType {
    //     ADD_GENE_TO_ORTHOLOG
    // }

    // public static enum EnrichPolicyType {
    //     SYNONYM2GENE
    // }

    // @Inject
    // ESHelper esHelper;

    // @Inject
    // ElasticsearchClient esClient;

    // @ConfigProperty(name = "geneservice.elasticsearch.force-recreate-enriched-index")
    // boolean forceRecreateEnrichedIndex;

    // public boolean exists(PipelineType pipelineType) throws IOException {
    //     String pipelineName = esHelper.getESName(pipelineType.name());
    //     return esClient.ingest().getPipeline(p -> p.id(pipelineName)).get(pipelineName) != null;
    // }

    // public void delete(PipelineType pipelineType) throws IOException {
    //     String pipelineName = esHelper.getESName(pipelineType.name());
    //     try {
    //         esClient.ingest().deletePipeline(p -> p.id(pipelineName));
    //     } catch (ElasticsearchException e) {
    //         if (e.status() != 404) {
    //             throw e;
    //         }
    //     }

    //     if (pipelineType == PipelineType.ADD_GENE_TO_ORTHOLOG) {
    //         String policyName = esHelper.getESName(EnrichPolicyType.SYNONYM2GENE.name());
    //         try {
    //             esClient.enrich().deletePolicy(p -> p.name(policyName));
    //         } catch (ElasticsearchException e) {
    //             if (e.status() != 404) {
    //                 throw e;
    //             }
    //         }
    //     }
    // }

    // public void createOrthologIngestPipeline() throws IOException {
    //     String policyName = esHelper.getESName(EnrichPolicyType.SYNONYM2GENE.name());
    //     String pipelineName = esHelper.getESName(PipelineType.ADD_GENE_TO_ORTHOLOG.name());
    //     String sourceIndex = esHelper.getESName(SourceIndexType.SYNONYM.name());

    //     boolean policyExists = esClient.enrich().getPolicy(p -> p.name(policyName)).policies().size() > 0;
    //     if (!policyExists) {
    //         esClient.enrich().putPolicy(p -> p
    //             .name(policyName)
    //             .match(m -> m
    //                 .indices(sourceIndex)
    //                 .matchField("value")
    //                 .enrichFields("gene")
    //             ));
    //         esClient.indices().refresh(r -> r
    //             .index(sourceIndex)
    //         );
    //         esClient.enrich().executePolicy(p -> p
    //             .name(policyName)
    //             .waitForCompletion(true)
    //         );
    //     }

    //     esClient.ingest().putPipeline(p -> p
    //         .id(pipelineName)
    //         .description("Enriches ortholog documents with current gene name")
    //         .processors(pr -> pr
    //             .enrich(e -> e
    //                 .policyName(policyName)
    //                 .field("ortholog")
    //                 .targetField("gene_enriched")
    //             ))
    //         .processors(pr -> pr
    //             .set(s -> s
    //                 .field("gene")
    //                 .value(JsonData.of("{{{gene_enriched.gene}}}"))
    //             ))
    //         .processors(pr -> pr
    //             .set(s -> s
    //                 .field("gene")
    //                 .value(JsonData.of("{{{ortholog}}}"))
    //                 .if_("ctx.gene == ''")
    //         ))
    //         .processors(pr -> pr
    //             .remove(r -> r
    //                 .field("gene_enriched")
    //                 .ignoreFailure(true)
    //             ))
    //     );
    // }
}
