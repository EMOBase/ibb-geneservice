package ibb.api.geneservice.domains.ortholog;

import java.io.IOException;
import java.io.UncheckedIOException;

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

	private String pipelineName;

	public OrthologIndex() {
		super("ortholog");
	}

	@PostConstruct
	@Override
	public void setup() {
		super.setup();
		pipelineName = getESHelper().getESName("add_gene_to_ortholog");
	}

	@Override
	public void delete() {
		super.delete();
		getESHelper().deletePipelineIgnoreUnavailable(pipelineName);
	}

	@Override
	protected TypeMapping getTypeMapping() {
        return null;
	}

	public String createPipeline() {
		String policyName = synonymIndex.getSynonym2GenePolicyName();
		try {
			if (getESHelper().enrichPolicyExists(policyName)) {
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
}
