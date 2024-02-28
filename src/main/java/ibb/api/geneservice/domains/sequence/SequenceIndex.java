package ibb.api.geneservice.domains.sequence;

import java.util.List;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import co.elastic.clients.elasticsearch.ingest.Processor;
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
            .properties("sequence", p -> p.text(tx -> tx.index(false)))
            .properties("name", p -> p.keyword(k -> k))
            .properties("species", p -> p.keyword(k -> k))
            .properties("type", p -> p.keyword(k -> k))
        );
	}

    @Override
    protected List<Processor> getPipelineProcessors() {
        return null;
    }

    @Override
    protected IndexSettingsAnalysis getAnalysis() {
        return null;
    }
}
