package ibb.api.geneservice.domains.ortholog;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.json.JsonData;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrthologIndex extends ESSourceIndex<Ortholog> {

	@Inject
	SynonymIndex synonymIndex;

    @ConfigProperty(name = "geneservice.elasticsearch.delete-ortholog-on-start", defaultValue = "false")
    boolean deleteOnStart;


	private String policyName;
	private String pipelineName;

	public OrthologIndex() {
		super("ortholog");
	}

	@PostConstruct
	@Override
	public void setup() {
		super.setup();
		policyName = getESHelper().getESName("synonym2gene");
		pipelineName = getESHelper().getESName("add_gene_to_ortholog");
	}

	@Override
	protected boolean shouldDeleteOnStart() {
        return deleteOnStart;
	}

	@Override
	protected TypeMapping getTypeMapping() {
        return null;
	}

	private boolean policyExists() {
		try {
			return getESClient().enrich().getPolicy(p -> p.name(policyName)).policies().size() > 0;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
    
	public void computeEnrichedIndex() {
		try {
			if (!policyExists()) {
				getESClient().enrich().putPolicy(p -> p
					.name(policyName)
					.match(m -> m
						.indices(synonymIndex.getQueryIndexName())
						.matchField("value")
						.enrichFields("gene")
					));
			}
			getESClient().enrich().executePolicy(p -> p
				.name(policyName)
				.waitForCompletion(true)
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public String createPipeline() {
		try {
			if (policyExists()) {
				getESClient().ingest().putPipeline(p -> p
					.id(pipelineName)
					.description("Enriches ortholog documents with current gene name")
					.processors(pr -> pr
						.enrich(e -> e
							.policyName(policyName)
							.field("ortholog")
							.targetField("gene_enriched")
						))
					.processors(pr -> pr
						.set(s -> s
							.field("gene")
							.value(JsonData.of("{{{gene_enriched.gene}}}"))
						))
					.processors(pr -> pr
						.set(s -> s
							.field("gene")
							.value(JsonData.of("{{{ortholog}}}"))
							.if_("ctx.gene == ''")
					))
					.processors(pr -> pr
						.remove(r -> r
							.field("gene_enriched")
							.ignoreFailure(true)
						))
				);
			} else {
				getESClient().ingest().putPipeline(p -> p
					.id(pipelineName)
					.processors(pr -> pr
						.set(s -> s
							.field("gene")
							.value(JsonData.of("{{{ortholog}}}"))
							.if_("ctx.gene == ''")
					))
				);
			}
			return pipelineName;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

// 	esClient.ingest().putPipeline(p -> p
// 	.id(pipelineName)
// 	.description("Enriches ortholog documents with current gene name")
// 	.processors(pr -> pr
// 		.enrich(e -> e
// 			.policyName(policyName)
// 			.field("ortholog")
// 			.targetField("gene_enriched")
// 		))
// 	.processors(pr -> pr
// 		.set(s -> s
// 			.field("gene")
// 			.value(JsonData.of("{{{gene_enriched.gene}}}"))
// 		))
// 	.processors(pr -> pr
// 		.set(s -> s
// 			.field("gene")
// 			.value(JsonData.of("{{{ortholog}}}"))
// 			.if_("ctx.gene == ''")
// 	))
// 	.processors(pr -> pr
// 		.remove(r -> r
// 			.field("gene_enriched")
// 			.ignoreFailure(true)
// 		))
// );

}
