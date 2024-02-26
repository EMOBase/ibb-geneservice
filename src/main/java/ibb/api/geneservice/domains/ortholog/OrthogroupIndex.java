package ibb.api.geneservice.domains.ortholog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.ingest.Processor;
import co.elastic.clients.json.JsonData;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.es.ESHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrthogroupIndex {

    private String indexName;
    private String policyName;
    private String pipelineName;
    private String transformName;

    @Inject
    ElasticsearchClient esClient;

    @Inject
    ESHelper esHelper;

    @Inject
    SynonymIndex synonymIndex;
    
    @Inject
    OrthologIndex orthologIndex;

    @PostConstruct
	public void setup() {
        indexName = esHelper.getESName("orthogroup");
		policyName = synonymIndex.getGene2SynonymsPolicyName();
		pipelineName = esHelper.getESName("add_synonyms_to_ortholog");
        transformName = esHelper.getESName("ortholog2orthogroup");
	}

    public void delete() {
        try {
            esClient.indices().delete(i -> i.index(indexName).ignoreUnavailable(true));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        esHelper.deleteTransformIgnoreUnavailable(transformName);
        esHelper.deletePipelineIgnoreUnavailable(pipelineName);
    }

    public void transform() {
        try {
            esClient.transform().putTransform(t -> t
                .transformId(transformName)
                .description("Group orthologs by their group")
                .source(s -> s.index(orthologIndex.getQueryIndexName()))
                .dest(d -> d
                    .index(indexName)
                    .pipeline(createPipeline())
                )
                .pivot(p -> p
                    .groupBy("group", g -> g
                        .terms(term -> term.field("group.keyword"))
                    )
                    .aggregations("orthologs", a -> a
                        .scriptedMetric(metric -> metric
                            .initScript(script -> script.inline(v -> v.source("state.genes = new LinkedHashSet()")))
                            .mapScript(script -> script.inline(v -> v.source("state.genes.add(doc['gene.keyword'].value)")))
                            .combineScript(script -> script.inline(v -> v.source("return state.genes")))
                            .reduceScript(script -> script.inline(v -> v.source("def acc = []; for (state in states) { acc.addAll(state) } return acc")))
                        )
                    )
                )
            );
            esClient.transform().startTransform(t -> t.transformId(transformName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

	public String createPipeline() {
        List<Processor> processors = new ArrayList<>();
        processors.add(Processor.of(pr -> pr
            .foreach(f -> f
                .field("orthologs")
                .processor(pr2 -> pr2
                    .set(s -> s
                        .field("_ingest._value")
                        .value(JsonData.of("{\"gene\": \"{{{_ingest._value}}}\"}"))
                    )
                )
            )
        ));
        processors.add(Processor.of(pr -> pr
            .json(j -> j.field("orthologs"))
        ));
		try {
			if (esHelper.enrichPolicyExists(policyName)) {
                processors.add(Processor.of(pr -> pr.foreach(f -> f
                    .field("orthologs")
                    .processor(pr2 -> pr2
                        .enrich(e -> e
                            .policyName(policyName)
                            .field("_ingest._value.gene")
                            .targetField("_ingest._value.synonyms")
                            .maxMatches(128)
                        )
                    )
                )));
                processors.add(Processor.of(pr -> pr.foreach(f -> f
                    .field("orthologs")
                    .processor(pr2 -> pr2.foreach(f2 -> f2
                        .field("_ingest._value.synonyms")
                        .processor(pr3 -> pr3.remove(r -> r
                            .field("_ingest._value.gene")
                        ))
                    ))
                )));
			}
            esClient.ingest().putPipeline(p -> p
                .id(pipelineName)
                .description("Enriches orthogroup documents with synonyms")
                .processors(processors)
            );
            return pipelineName;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
