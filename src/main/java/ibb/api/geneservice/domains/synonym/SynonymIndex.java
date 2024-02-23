package ibb.api.geneservice.domains.synonym;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SynonymIndex extends ESSourceIndex<Synonym> {

	@ConfigProperty(name = "geneservice.elasticsearch.delete-synonym-on-start", defaultValue = "false")
    boolean deleteOnStart;

    public SynonymIndex() {
		super("synonym");
	}

	@Override
	protected boolean shouldDeleteOnStart() {
        return deleteOnStart;
	}

	@Override
	protected TypeMapping getTypeMapping() {
        return null;
	}
}
