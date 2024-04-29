package ibb.api.geneservice.domains.dsrna;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.utils.Species;

public class DsRNAParser extends TextParser<DsRNA> {

    private Species species;
    public DsRNAParser(Species species) {
        this.species = species;
    }

    @Override
    public Stream<DsRNA> parse(Path path) throws IOException {
        return parseText(path)
            .filter(line -> !isHeaderLine(line))
            .filter(line -> !line.isBlank())
            .map(this::parseLine);
    }
    
    private DsRNA parseLine(String line) {
        final String delimiter = ",";
        String[] cols = line.split(delimiter);
        if (cols.length < 2) {
            throw new IllegalArgumentException("dsRNA sequence file must have at least 2 columns");
        }
        DsRNA dsRNA = new DsRNA();
        dsRNA.id = cols[0].trim();
        dsRNA.species = species;
        dsRNA.seq = cols[1].trim();

        if (cols.length > 2) {
            dsRNA.leftPrimer = cols[2].trim();
        }
        if (cols.length > 3) {
            dsRNA.rightPrimer = cols[3].trim();
        }
        return dsRNA;
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }
}
