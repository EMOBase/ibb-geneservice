package ibb.api.geneservice.domains.synonym.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import ibb.api.geneservice.utils.Species;

public class IBTCParser extends TextParser<Synonym> {

    private Species species;
    
    public IBTCParser(Species species) {
        this.species = species;
    }

    @Override
    public Stream<Synonym> parse(Path path) throws IOException {
        return parseText(path)
            .filter(line -> !isHeaderLine(line))
            .filter(line -> !line.isBlank())
            .map(this::parseLine);
    }

    public Synonym parseLine(String line) {
        final String delimiter = ",";

        String[] cols = line.split(delimiter);
        if (cols.length != 2) {
            throw new TextParserException(getLineNumber(), "Mapping file for iB and TC must have exactly 2 columns");
        }

        String gene = species.createGeneId(cols[1].trim());
        return new Synonym(gene, Synonym.Type.DSRNA, cols[0].trim());
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    } 
}
