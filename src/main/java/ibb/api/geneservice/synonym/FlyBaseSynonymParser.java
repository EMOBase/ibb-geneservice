package ibb.api.geneservice.synonym;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;

public class FlyBaseSynonymParser implements SynonymParser {

    @Override
    public Stream<Synonym> parse(Path path) throws IOException {
        return TextParser.parse(path)
            .filter(line -> !isHeaderLine(line))
            .filter(line -> !line.isBlank())
            .map(this::parseLine)
            .flatMap(List::stream)
            .filter(s -> s.value != null && !s.value.isBlank());
    }

    public List<Synonym> parseLine(String line) {
        final String delimiter = "\t";

        String[] cols = line.split(delimiter);
        if (cols.length < 2) {
            throw new IllegalArgumentException("FlyBase synonym file must have at least 2 columns");
        }

        String geneId = cols[0];
        String species = cols[1];
        if (!Objects.equals("Dmel", species) || !geneId.startsWith("FBgn")) {
            return List.of();
        }

        List<Synonym> synonyms = new ArrayList<>();
        if (cols.length > 2) {
            synonyms.add(new Synonym(geneId, "symbol", cols[2]));
        }
        if (cols.length > 3) {
            synonyms.add(new Synonym(geneId, "name", cols[3]));
        }
        if (cols.length > 4) {
            Arrays.stream(cols[4].split("|"))
                .forEach(val -> synonyms.add(new Synonym(geneId, "other_symbols", val)));
        }
        if (cols.length > 5) {
            Arrays.stream(cols[5].split("|"))
                .forEach(val -> synonyms.add(new Synonym(geneId, "other_names", val)));
        }

        return synonyms;
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }
}
