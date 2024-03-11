package ibb.api.geneservice.domains.dsrna;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DsRNAIndex extends ESSourceIndex<DsRNA>{
    
    @ConfigProperty(name = "geneservice.elasticsearch.delete-on-start.dsrnas", defaultValue = "false")
    boolean shouldDeleteOnStart;

    public DsRNAIndex() {
        super("dsrna", DsRNA.class);
    }

	@Override
	protected TypeMapping getTypeMapping() {
        return TypeMapping.of(m -> m
            .properties("seq", p -> p.text(tx -> tx.index(false)))
            .properties("leftPrimer", p -> p.text(tx -> tx.index(false)))
            .properties("rightPrimer", p -> p.text(tx -> tx.index(false)))
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
