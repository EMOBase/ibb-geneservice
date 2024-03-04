package ibb.api.geneservice.domains.synonym;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import ibb.api.geneservice.es.ESDocSourceProvider;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SynonymIndex extends ESSourceIndex<Synonym> {

    @ConfigProperty(name = "geneservice.elasticsearch.delete-on-start.synonyms", defaultValue = "false")
    boolean shouldDeleteOnStart;

    @Inject
    ESDocSourceProvider<Synonym> docSourceProvider;

    public SynonymIndex() {
		super("synonym");
	}

	@Override
	protected TypeMapping getTypeMapping() {
        return TypeMapping.of(t -> t
			.properties("synonym", p -> p.searchAsYouType(s -> s
				.analyzer("whitespace_lowercase")
				.fields("keyword", k -> k.keyword(kk -> kk))
			))
		);
	}

	@Override
	protected IndexSettingsAnalysis getAnalysis() {
        return IndexSettingsAnalysis.of(a -> a
			.analyzer("whitespace_lowercase", getESHelper().getWhitespaceLowercaseAnalyzer())
        );
	}

	@Override
	protected boolean shouldDeleteOnStart() {
		return shouldDeleteOnStart;
	}
}
