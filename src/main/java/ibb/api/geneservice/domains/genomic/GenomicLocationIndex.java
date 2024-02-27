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
		return TypeMapping.of(m -> m
			.properties("species", p -> p.keyword(k -> k))
			.properties("gene", p -> p.keyword(k -> k))
			.properties("referenceSeq", p -> p.keyword(k -> k.index(false)))
			.properties("start", p -> p.integer(i -> i.index(false)))
			.properties("end", p -> p.integer(i -> i.index(false)))
			.properties("strand", p -> p.keyword(k -> k.index(false)))
		);
	}
}
