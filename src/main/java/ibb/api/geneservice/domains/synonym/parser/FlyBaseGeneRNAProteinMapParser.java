package ibb.api.geneservice.domains.synonym.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import ibb.api.geneservice.utils.Species;

public class FlyBaseGeneRNAProteinMapParser extends TextParser<Synonym> {
    private Species species;

    public FlyBaseGeneRNAProteinMapParser(Species species) {
        this.species = species;
    }

    @Override
    public Stream<Synonym> parse(Path path) throws IOException {
        return parseText(path)
            .filter(line -> !isHeaderLine(line))
            .filter(line -> !line.isBlank())
            .map(this::parseLine)
            .flatMap(List::stream);
    }

    private List<Synonym> parseLine(String line) {
        final String delimiter = "\t";

        String[] cols = line.split(delimiter);
        if (cols.length < 2) {
            throw new TextParserException(getLineNumber(), "FlyBase gene RNA protein map file must have at least 2 columns");
        }

        String gene = species.createGeneId(cols[0]);
        String transcript = cols[1];

        var synonyms = new ArrayList<Synonym>();
        synonyms.add(new Synonym(gene, Synonym.Type.CURRENT_ID, cols[0]));

        if (!transcript.isBlank()) {
            synonyms.add(new Synonym(gene, Synonym.Type.TRANSCRIPT, transcript));
        }

        if (cols.length >= 3) {
            String protein = cols[2];
            if (!protein.isBlank()) {
                synonyms.add(new Synonym(gene, Synonym.Type.PROTEIN, protein));
            }
        }
        return synonyms;
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }
}
