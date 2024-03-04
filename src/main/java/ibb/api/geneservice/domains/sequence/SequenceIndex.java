package ibb.api.geneservice.domains.sequence;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SequenceIndex extends ESSourceIndex<Sequence> {

    @ConfigProperty(name = "geneservice.elasticsearch.delete-on-start.sequences", defaultValue = "false")
    boolean shouldDeleteOnStart;

    public SequenceIndex() {
        super("sequence");
    }

	@Override
	protected TypeMapping getTypeMapping() {
        return TypeMapping.of(m -> m
            .properties("sequence", p -> p.text(tx -> tx.index(false)))
        );
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
