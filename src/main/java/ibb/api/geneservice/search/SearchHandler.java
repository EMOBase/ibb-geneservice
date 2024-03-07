package ibb.api.geneservice.search;

import java.util.List;
import java.util.Optional;

import ibb.api.geneservice.domains.genomic.GenomicLocationIndex;
import ibb.api.geneservice.domains.orthology.Orthology;
import ibb.api.geneservice.domains.orthology.OrthologyIndex;
import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.domains.synonym.SynonymSuggestResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SearchHandler {

    public static class SearchResult {
        public Optional<String> gene;
        public List<Synonym> synonyms;
        public List<Orthology> orthologies;
    }

    @Inject
    GenomicLocationIndex genomicLocationIndex;

    @Inject
    SynonymIndex synonymIndex;

    @Inject
    OrthologyIndex orthologyIndex;

    public SynonymSuggestResult suggest(String query, List<String> searchAfter) {
        return synonymIndex.suggest(query, searchAfter);
    }

    public SearchResult search(String query) {
        var searchResult = new SearchResult();

        searchResult.gene = genomicLocationIndex.findById(query).map(s -> s.gene);
        List<Synonym> synonyms = synonymIndex.findBySynonym(query);
        searchResult.synonyms = synonyms;

        if (!synonyms.isEmpty()) {
            List<String> orthologs = synonyms.stream()
                .map(s -> s.gene)
                .distinct()
                .toList();
        
            searchResult.orthologies = orthologyIndex.listByOrthologs(orthologs);
        } else {
            searchResult.orthologies = List.of();
        }
        return searchResult;
    }
}
