package ibb.api.geneservice.domains.sequence;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SequenceIndex extends ESSourceIndex<Sequence> {

    @ConfigProperty(name = "geneservice.elasticsearch.delete-sequence-on-start", defaultValue = "false")
    boolean deleteOnStart;

    public SequenceIndex() {
        super("sequence");
    }

    @Override
    protected boolean shouldDeleteOnStart() {
        return deleteOnStart;
    }

	@Override
	protected TypeMapping getTypeMapping() {
        return TypeMapping.of(m -> m
            .properties("sequence", pr -> pr.text(tx -> tx.index(false)))
        );
	}
}
