package ibb.api.geneservice.domains.synonym;

import java.io.IOException;
import java.io.UncheckedIOException;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SynonymIndex extends ESSourceIndex<Synonym> {

	private String gene2SynonymsPolicyName;

    public SynonymIndex() {
		super("synonym");
	}

	@Override
	@PostConstruct
	protected void init() {
		super.init();
		gene2SynonymsPolicyName = getESHelper().getESName("gene2synonym");
	}

	@Override
	public void delete() {
		super.delete();
		getESHelper().deleteEnrichPolicyIgnoreUnavailable(gene2SynonymsPolicyName);
	}

	@Override
	protected TypeMapping getTypeMapping() {
        return TypeMapping.of(t -> t
			.properties("species", p -> p.keyword(k -> k))
			.properties("gene", p -> p.keyword(k -> k))
			.properties("type", p -> p.keyword(k -> k))
			.properties("value", p -> p.text(tx -> tx))
		);
	}

	public String getGene2SynonymsPolicyName() {
		return gene2SynonymsPolicyName;
	}

	public void computeGene2SynonymsEnrichedIndex() {
		String policyName = getGene2SynonymsPolicyName();
		try {
			getESClient().enrich().putPolicy(p -> p
				.name(policyName)
				.match(m -> m
					.indices(getQueryIndexName())
					.matchField("gene")
					.enrichFields("type", "value", "species")
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
