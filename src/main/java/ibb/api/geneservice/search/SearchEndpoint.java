package ibb.api.geneservice.search;

import java.io.IOException;
import java.util.List;

import org.jboss.resteasy.reactive.RestQuery;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import ibb.api.geneservice.domains.genomic.GenomicLocationIndex;
import ibb.api.geneservice.domains.orthology.OrthologyIndex;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
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
    public SearchResult<SuggestItem> suggest(
        @RestQuery("q") String query,
        @RestQuery("searchAfter") List<String> searchAfter
    ) throws IOException {

        if (query == null || query.isBlank()) {
            throw new BadRequestException("Query is required");
        }

        var requestBuilder = new SearchRequest.Builder()
            .index(synonymIndex.getQueryIndexName())
            .collapse(c -> c.field("synonym.keyword"))
            .sort(s -> s.field(f -> f.field("synonym.keyword").order(SortOrder.Asc)))
            .query(q2 -> q2
                .multiMatch(m -> m
                    .query(query)
                    .type(TextQueryType.BoolPrefix)
                    .fields(
                        "synonym",
                        "synonym._2gram",
                        "synonym._3gram"
                    ))
            );
        if (searchAfter != null && !searchAfter.isEmpty()) {
            if (searchAfter.size() != 1) {
                throw new BadRequestException("searchAfter must contain exactly one value");
            }
            requestBuilder.searchAfter(searchAfter.stream().map(FieldValue::of).toList());
        }
        try {
            var response = esClient.search(requestBuilder.build(), SuggestItem.class);
            return SearchResult.of(response);
        } catch (ElasticsearchException e) {
            Log.debug(e.error().causedBy());
            throw e;
        }
    }

    @GET
    public SearchResult<OrthologySearchItem> searchOrthology(
        @RestQuery("q") String query,
        @RestQuery("searchAfter") List<String> searchAfter
    ) throws IOException {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Query is required");
        }

        var requestBuilder = new SearchRequest.Builder()
            .index(orthologyIndex.getQueryIndexName())
            .sort(s -> s.field(f -> f.field("priority").order(SortOrder.Asc)))
            .sort(s -> s.field(f -> f.field("source").order(SortOrder.Asc)))
            .sort(s -> s.score(s2 -> s2.order(SortOrder.Desc)))
            .sort(s -> s.field(f -> f.field("group").order(SortOrder.Asc)))
            .query(q -> q.simpleQueryString(s -> s
                .query(query)
                .defaultOperator(Operator.And))
            );

        if (searchAfter != null && !searchAfter.isEmpty()) {
            if (searchAfter.size() != 4) {
                throw new BadRequestException("searchAfter must contain exactly 4 values");
            }
            try {
                var priority = Integer.parseInt(searchAfter.get(0));
                var score = Double.parseDouble(searchAfter.get(2));
                requestBuilder.searchAfter(List.of(
                    FieldValue.of(priority),
                    FieldValue.of(searchAfter.get(1)),
                    FieldValue.of(score),
                    FieldValue.of(searchAfter.get(3))
                ));
            } catch (NumberFormatException e) {
                throw new BadRequestException("searchAfter[0] and searchAfter[2] must be a number");
            }
        }
        try {
            var response = esClient.search(requestBuilder.build(), OrthologySearchItem.class);
            return SearchResult.of(response);
        } catch (ElasticsearchException e) {
            Log.debug(e.error().causedBy());
            throw e;
        }
    }
}
