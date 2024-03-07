package ibb.api.geneservice.domains.orthology;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrthologyIndex extends ESSourceIndex<Orthology> {

    @ConfigProperty(name = "geneservice.elasticsearch.delete-on-start.orthology", defaultValue = "false")
    boolean shouldDeleteOnStart;

    public OrthologyIndex() {
        super("orthology", Orthology.class);
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

    public List<Orthology> listByOrthologs(List<String> orthologs) {
        List<FieldValue> terms = orthologs.stream().map(FieldValue::of).toList();
        var requestBuilder = new SearchRequest.Builder()
            .query(q -> q.terms(t -> t.field("orthologs.keyword").terms(tt -> tt.value(terms))));
        return search(requestBuilder).hits().hits().stream().map(h -> h.source()).toList();
    }
}
