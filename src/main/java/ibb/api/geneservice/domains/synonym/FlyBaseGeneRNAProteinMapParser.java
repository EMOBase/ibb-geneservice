package ibb.api.geneservice.domains.synonym;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;

public class FlyBaseGeneRNAProteinMapParser implements TextParser<Synonym> {

    private AtomicInteger lineCount = new AtomicInteger(0);
    private String species;

    public FlyBaseGeneRNAProteinMapParser(String species) {
        this.species = species;
    }

    @Override
    public Stream<Synonym> parse(Path path) throws IOException {
        return parseText(path)
            .map(line -> {
                lineCount.incrementAndGet();
                return line;
            })
            .filter(line -> !isHeaderLine(line))
            .filter(line -> !line.isBlank())
            .map(this::parseLine)
            .flatMap(List::stream);
    }

    private List<Synonym> parseLine(String line) {
        final String delimiter = "\t";

        String[] cols = line.split(delimiter);
        if (cols.length < 2) {
            throw new TextParserException(lineCount.get(), "FlyBase gene RNA protein map file must have at least 2 columns");
        }

        String gene = cols[0];
        String transcript = cols[1];

        var synonyms = new ArrayList<Synonym>();

        if (!transcript.isBlank()) {
            synonyms.add(new Synonym(species, gene, Synonym.Type.TRANSCRIPT, transcript));
        }

        if (cols.length >= 3) {
            String protein = cols[2];
            if (!protein.isBlank()) {
                synonyms.add(new Synonym(species, gene, Synonym.Type.PROTEIN, protein));
            }
        }
        return synonyms;
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }
}
