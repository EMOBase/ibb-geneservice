package ibb.api.geneservice.domains.synonym;

import java.io.IOException;
import java.util.List;

import org.jboss.resteasy.reactive.RestQuery;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import ibb.api.geneservice.search.SuggestItem;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/synonym")
public class SynonymAPI {

    @Inject
    SynonymIndex synonymIndex;

    @Inject
    ElasticsearchClient esClient;

    @GET
    @Path("/_suggest")
    public SearchResult<SuggestItem> suggest(
        @RestQuery("q") @NotBlank String query,
        @RestQuery("searchAfter") List<@NotBlank String> searchAfter
    ) throws IOException {

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
}

class SearchResult<T> {
    public long total;
    public List<T> hits;
    public List<Object> searchAfter;

    public static <T> SearchResult<T> of(SearchResponse<T> response) {
        var result = new SearchResult<T>();
        var hits = response.hits().hits();
        result.hits = hits.stream().map(h -> h.source()).toList();
        result.total = response.hits().total().value();
        if (hits.size() > 0) {
            var lastHit = hits.get(hits.size() - 1);
            var searchAfter = lastHit.sort().stream().map(s -> s._get()).toList();
            if (searchAfter.size() > 0) {
                result.searchAfter = searchAfter;
            }
        }
        return result;
    }
}
