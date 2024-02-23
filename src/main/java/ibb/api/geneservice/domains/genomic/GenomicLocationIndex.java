package ibb.api.geneservice.domains.genomic;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GenomicLocationIndex extends ESSourceIndex<GenomicLocation> {

    @ConfigProperty(name = "geneservice.elasticsearch.delete-genomiclocation-on-start", defaultValue = "false")
    boolean deleteOnStart;

    public GenomicLocationIndex() {
        super("genomiclocation");
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
