package ibb.api.geneservice.domains.orthology;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrthologyIndex extends ESSourceIndex<Orthology> {

    @ConfigProperty(name = "geneservice.elasticsearch.delete-on-start.orthology", defaultValue = "false")
    boolean shouldDeleteOnStart;

    public OrthologyIndex() {
        super("orthology");
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
