package ibb.api.geneservice.domains.sequence;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SequenceIndex extends ESSourceIndex<Sequence> {

    public SequenceIndex() {
        super("sequence");
    }

	@Override
	protected TypeMapping getTypeMapping() {
        return TypeMapping.of(m -> m
            .properties("sequence", pr -> pr.text(tx -> tx.index(false)))
        );
	}
}
