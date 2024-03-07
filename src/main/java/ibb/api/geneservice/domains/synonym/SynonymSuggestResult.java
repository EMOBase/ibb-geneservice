package ibb.api.geneservice.domains.synonym;

import java.util.List;

import co.elastic.clients.elasticsearch.core.SearchResponse;

public class SynonymSuggestResult {
    public long total;
    public List<Synonym> hits;
    public List<Object> searchAfter;

    public static SynonymSuggestResult of(SearchResponse<Synonym> response) {
        var result = new SynonymSuggestResult();
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
