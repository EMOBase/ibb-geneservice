package ibb.api.geneservice.domains.genomic;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GenomicLocationIndex extends ESSourceIndex<GenomicLocation> {

    public GenomicLocationIndex() {
        super("genomiclocation");
    }

	@Override
	protected TypeMapping getTypeMapping() {
		return null;
	}
}
