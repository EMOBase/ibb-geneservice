package ibb.api.geneservice.webapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ibb.api.geneservice.domains.orthology.Orthology;
import ibb.api.geneservice.domains.orthology.OrthologyIndex;
import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.utils.Species;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SearchHandler {

    public static class OrthologyWithSynonyms {
        public static class GeneGroup {
            public static class GeneWithSynonyms {
                public String gene;
                public List<String> synonyms;
            }
            public Species species;
            public List<GeneWithSynonyms> genes;
        }

        public String group;
        public String source;
        public List<GeneGroup> orthologs;
    }

    public static class OtherGene {
        public Species species;
        public String gene;
    }

    public static class SearchResult {
        public List<String> genes = new ArrayList<>();
        public List<OrthologyWithSynonyms> orthologies;
        public List<OtherGene> otherGenes;
    }

    @ConfigProperty(name = "geneservice.main-species")
    Species mainSpecies;

    @Inject
    SynonymIndex synonymIndex;

    @Inject
    OrthologyIndex orthologyIndex;

    public List<String> suggest(String query) {
        return synonymIndex.suggest(query);
    }

    public SearchResult search(String query) {
        Map<Boolean, List<Synonym>> synonyms = synonymIndex.findBySynonym(query)
            .stream()
            .collect(Collectors.groupingBy(s -> mainSpecies.isGeneFromSpecies(s.gene), Collectors.toList()));

        List<Synonym> mainSpeciesSynonyms = synonyms.get(true);
        if (mainSpeciesSynonyms != null) {
            List<String> genes = mainSpeciesSynonyms.stream()
                .map(s -> mainSpecies.removeSpeciesFromGene(s.gene))
                .distinct()
                .toList();
            var searchResult = new SearchResult();
            searchResult.genes = genes;
            return searchResult;
        }

        List<Synonym> otherSpeciesSynonyms = synonyms.get(false);
        if (otherSpeciesSynonyms != null) {
            Map<String, List<Synonym>> geneToSynonyms = otherSpeciesSynonyms.stream().collect(Collectors.groupingBy(s -> s.gene));
            List<String> orthologs = otherSpeciesSynonyms.stream()
                .map(s -> s.gene)
                .distinct()
                .toList();
            List<OrthologyWithSynonyms> orthologies = orthologyIndex.listByOrthologs(orthologs)
                .stream()
                .map(orthology -> enrichOrthology(orthology, geneToSynonyms))
                .toList();

            if (!orthologies.isEmpty()) {
                var searchResult = new SearchResult();
                searchResult.orthologies = orthologies;
                return searchResult;
            } else {
                var searchResult = new SearchResult();
                searchResult.otherGenes = otherSpeciesSynonyms.stream()
                    .map(s -> s.gene)
                    .distinct()
                    .map(gene -> {
                        var otherGene = new OtherGene();
                        otherGene.species = Species.ofGene(gene);
                        otherGene.gene = otherGene.species.removeSpeciesFromGene(gene);
                        return otherGene;
                    })
                    .toList();
                return searchResult;
            }
        }

        return new SearchResult();
    }

    private OrthologyWithSynonyms enrichOrthology(Orthology orthology, Map<String, List<Synonym>> geneToSynonyms) {
        Map<Species, List<OrthologyWithSynonyms.GeneGroup.GeneWithSynonyms>> speciesToGenes = new HashMap<>();
        for (String gene : orthology.orthologs) {
            Species species = Species.ofGene(gene);
            var geneWithSynonyms = new OrthologyWithSynonyms.GeneGroup.GeneWithSynonyms();
            geneWithSynonyms.synonyms = geneToSynonyms.getOrDefault(gene, List.of())
                .stream()
                .filter(s -> s.type != Synonym.Type.CURRENT_ID)
                .map(s -> s.synonym)
                .toList();
            geneWithSynonyms.gene = species.removeSpeciesFromGene(gene);
            speciesToGenes.computeIfAbsent(species, k -> new ArrayList<>())
                .add(geneWithSynonyms);
        }

        OrthologyWithSynonyms orthologyWithSynonyms = new OrthologyWithSynonyms();
        String[] groupParts = orthology.group.split(":", 2);
        orthologyWithSynonyms.source = groupParts[0].split("\\.")[1];
        orthologyWithSynonyms.group = groupParts[1];
        orthologyWithSynonyms.orthologs = speciesToGenes.entrySet().stream()
            .map(e -> {
                var geneGroup = new OrthologyWithSynonyms.GeneGroup();
                geneGroup.species = e.getKey();
                geneGroup.genes = e.getValue();
                return geneGroup;
            })
            .sorted((a, b) -> {
                if (a.species.equals(mainSpecies) && !b.species.equals(mainSpecies)) {
                    return -1;
                } else if (!a.species.equals(mainSpecies) && b.species.equals(mainSpecies)) {
                    return 1;
                } else {
                    return a.species.toString().compareTo(b.species.toString());
                }
            })
            .toList();
        return orthologyWithSynonyms;
    }
}
