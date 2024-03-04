package ibb.api.geneservice.domains.genomic;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GenomicLocationIndex extends ESSourceIndex<GenomicLocation> {

    @ConfigProperty(name = "geneservice.elasticsearch.delete-on-start.genomiclocations", defaultValue = "false")
    boolean shouldDeleteOnStart;

    public GenomicLocationIndex() {
        super("genomiclocation");
    }

	@Override
	protected TypeMapping getTypeMapping() {
		return null;
	}

	@Override
	protected IndexSettingsAnalysis getAnalysis() {
		return null;
	}

	@Override
	protected boolean shouldDeleteOnStart() {
		return shouldDeleteOnStart;
	}
}
