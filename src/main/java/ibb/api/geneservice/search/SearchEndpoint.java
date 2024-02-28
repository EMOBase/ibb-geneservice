package ibb.api.geneservice.search;

import java.io.IOException;
import java.util.List;

import org.jboss.resteasy.reactive.RestQuery;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import ibb.api.geneservice.domains.genomic.GenomicLocationIndex;
import ibb.api.geneservice.domains.orthology.OrthologyIndex;
import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/search")
public class SearchEndpoint {

    @Inject
    OrthologyIndex orthologyIndex;

    @Inject
    GenomicLocationIndex genomicLocationIndex;

    @Inject
    SynonymIndex synonymIndex;

    @Inject
    ElasticsearchClient esClient;

    @GET
    @Path("/_suggest")
    public SearchResult<Synonym> suggest(@RestQuery("q") String query) throws IOException {
        var response = esClient.search(r -> r
            .query(q -> q.functionScore(f -> f
                .functions(f2 -> f2.scriptScore(s -> s
                    .script(s2 -> s2.inline(i -> i.source(
                        "('NAME'.equals(doc['type'].value) || 'SYMBOL'.equals(doc['type'].value)) ? 5 : 1"
                    )))
                ))
                .query(q2 -> q2.multiMatch(m -> m
                    .query(query)
                    .type(TextQueryType.BoolPrefix)
                    .fields(
                        "synonym",
                        "synonym._2gram",
                        "synonym._3gram"
                    )
                ))
            ))
        , Synonym.class);
        return SearchResult.of(response);
    }

    @GET
    @Path("/orthology")
    public SearchResult<OrthologySearchItem> searchOrthology(@RestQuery String q) throws IOException {
        var response = esClient.search(r -> r
            .index(orthologyIndex.getQueryIndexName())
            .q(q)
            .defaultOperator(Operator.And)
        , OrthologySearchItem.class);
        return SearchResult.of(response);
    }

    @GET
    @Path("/gene")
    public SearchResult<GeneSearchItem> searchGene(@RestQuery String q) throws IOException {
        List<String> indices = List.of(
            synonymIndex.getQueryIndexName(),
            genomicLocationIndex.getQueryIndexName()
        );

        var response = esClient.search(r -> r.index(indices).q(q), GeneSearchItem.class);
        return SearchResult.of(response);
    }
}
