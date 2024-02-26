package ibb.api.geneservice.domains.synonym;

import java.io.IOException;
import java.io.UncheckedIOException;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SynonymIndex extends ESSourceIndex<Synonym> {

	private String synonym2GenePolicyName;
	private String gene2SynonymsPolicyName;

    public SynonymIndex() {
		super("synonym");
	}

	@Override
	@PostConstruct
	public void setup() {
		super.setup();
		synonym2GenePolicyName = getESHelper().getESName("synonym2gene");
		gene2SynonymsPolicyName = getESHelper().getESName("gene2synonym");
	}

	@Override
	public void delete() {
		super.delete();
		getESHelper().deleteEnrichPolicyIgnoreUnavailable(gene2SynonymsPolicyName);
		getESHelper().deleteEnrichPolicyIgnoreUnavailable(synonym2GenePolicyName);
	}

	@Override
	protected TypeMapping getTypeMapping() {
        return null;
	}

	public String getGene2SynonymsPolicyName() {
		return gene2SynonymsPolicyName;
	}

	public String getSynonym2GenePolicyName() {
		return synonym2GenePolicyName;
	}

	public void computeSynonym2GeneEnrichedIndex() {
		String policyName = getSynonym2GenePolicyName();
		try {
			getESClient().enrich().putPolicy(p -> p
				.name(policyName)
				.match(m -> m
					.indices(getQueryIndexName())
					.matchField("value")
					.enrichFields("gene")
				));
			getESClient().enrich().executePolicy(p -> p
				.name(policyName)
				.waitForCompletion(true)
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void computeGene2SynonymsEnrichedIndex() {
		String policyName = getGene2SynonymsPolicyName();
		try {
			getESClient().enrich().putPolicy(p -> p
				.name(policyName)
				.match(m -> m
					.indices(getQueryIndexName())
					.matchField("gene")
					.enrichFields("type", "value")
				));
			getESClient().enrich().executePolicy(p -> p
				.name(policyName)
				.waitForCompletion(true)
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
