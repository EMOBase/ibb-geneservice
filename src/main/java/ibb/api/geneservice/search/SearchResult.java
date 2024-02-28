package ibb.api.geneservice.search;

import java.util.List;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

public class SearchResult<T> {
    public long total;
    public List<T> hits;

    public static <T> SearchResult<T> of(SearchResponse<T> response) {
        var result = new SearchResult<T>();
        result.hits = response.hits().hits().stream().map(Hit::source).toList();
        result.total = response.hits().total().value();
        return result;
    }
}
