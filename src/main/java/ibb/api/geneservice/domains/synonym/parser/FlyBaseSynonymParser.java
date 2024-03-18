package ibb.api.geneservice.domains.synonym.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import ibb.api.geneservice.utils.Species;

public class FlyBaseSynonymParser extends TextParser<Synonym> {

    private Species species;

    public FlyBaseSynonymParser(Species species) {
        this.species = species;
    }

    @Override
    public Stream<Synonym> parse(Path path) throws IOException {
        return parseText(path)
            .filter(line -> !isHeaderLine(line))
            .filter(line -> !line.isBlank())
            .map(this::parseLine)
            .flatMap(List::stream)
            .filter(s -> s.synonym != null && !s.synonym.isBlank() && !isEngineeredFusionGene(s.synonym));
    }

    public List<Synonym> parseLine(String line) {
        final String delimiter = "\t";

        String[] cols = line.split(delimiter);
        if (cols.length < 2) {
            throw new TextParserException(getLineNumber(), "FlyBase synonym file must have at least 2 columns");
        }

        String infileGene = cols[0];
        Species infileSpecies;
        try {
            infileSpecies = Species.of(cols[1]);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        if (!Objects.equals(species, infileSpecies) || !infileGene.startsWith("FBgn")) {
            return List.of();
        }

        String gene = species.createGeneId(infileGene);
        List<Synonym> synonyms = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        synonyms.add(new Synonym(gene, Synonym.Type.CURRENT_ID, infileGene));
        if (cols.length > 2) {
            synonyms.add(new Synonym(gene, Synonym.Type.SYMBOL, cols[2]));
            seen.add(normalizeSynonym(cols[2]));
        }
        if (cols.length > 3) {
            synonyms.add(new Synonym(gene, Synonym.Type.NAME, cols[3]));
            seen.add(normalizeSynonym(cols[3]));
        }

        List<String> others = new ArrayList<>();
        if (cols.length > 4) {
            others.addAll(Arrays.asList(cols[4].split("\\|")));
        }
        if (cols.length > 5) {
            others.addAll(Arrays.asList(cols[5].split("\\|")));
        }
        Collections.sort(others, Comparator.reverseOrder());
        for (String other : others) {
            String normalized = normalizeSynonym(other);
            if (!seen.contains(normalized)) {
                synonyms.add(new Synonym(gene, Synonym.Type.OTHER, other));
                seen.add(normalized);
            }
        }
        return synonyms;
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }

    private String normalizeSynonym(String synonym) {
        return synonym.toLowerCase().trim().replace("-", " ");
    }

    private boolean isEngineeredFusionGene(String gene) {
        return gene.contains("::");
    }
}
