package ibb.api.geneservice.search;

import java.util.List;

import co.elastic.clients.elasticsearch.core.SearchResponse;

public class SearchResult<T> {
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
            result.searchAfter = lastHit.sort().stream().map(s -> s._get()).toList();
        }
        return result;
    }
}
