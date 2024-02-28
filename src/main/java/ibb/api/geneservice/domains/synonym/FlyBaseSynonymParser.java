package ibb.api.geneservice.domains.synonym;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;

public class FlyBaseSynonymParser implements TextParser<Synonym> {

    private String species;

    public FlyBaseSynonymParser(String species) {
        this.species = species;
    }

    @Override
    public Stream<Synonym> parse(Path path) throws IOException {
        return parseText(path)
            .filter(line -> !isHeaderLine(line))
            .filter(line -> !line.isBlank())
            .map(this::parseLine)
            .flatMap(List::stream)
            .filter(s -> s.synonym != null && !s.synonym.isBlank());
    }

    public List<Synonym> parseLine(String line) {
        final String delimiter = "\t";

        String[] cols = line.split(delimiter);
        if (cols.length < 2) {
            throw new IllegalArgumentException("FlyBase synonym file must have at least 2 columns");
        }

        String geneId = cols[0];
        String infileSpecies = cols[1];
        if (!Objects.equals(species, infileSpecies) || !geneId.startsWith("FBgn")) {
            return List.of();
        }

        List<Synonym> synonyms = new ArrayList<>();
        if (cols.length > 2) {
            synonyms.add(new Synonym(species, geneId, Synonym.Type.SYMBOL, cols[2]));
        }
        if (cols.length > 3) {
            synonyms.add(new Synonym(species, geneId, Synonym.Type.NAME, cols[3]));
        }
        if (cols.length > 4) {
            Arrays.stream(cols[4].split("\\|"))
                .forEach(val -> synonyms.add(new Synonym(species, geneId, Synonym.Type.OTHER_SYMBOL, val)));
        }
        if (cols.length > 5) {
            Arrays.stream(cols[5].split("\\|"))
                .forEach(val -> synonyms.add(new Synonym(species, geneId, Synonym.Type.OTHER_NAME, val)));
        }

        return synonyms;
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }
}
