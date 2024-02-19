package ibb.api.geneservice.domains.ortholog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;

public class OrthologParser implements TextParser<Ortholog> {

    @Override
    public Stream<Ortholog> parse(Path path) throws IOException {
        return parseText(path)
            .skip(1)
            .filter(line -> !line.isBlank())
            .map(this::parseLine)
            .flatMap(List::stream);
    }
    
    private List<Ortholog> parseLine(String line) {
        final String delimiter = "\t";

        String[] cols = line.split(delimiter);
        if (cols.length < 3) {
            throw new IllegalArgumentException("Ortholog file must have at least 3 columns");
        }

        String group = cols[0];
        return Arrays.stream(Arrays.copyOfRange(cols, 1, cols.length))
            .map(col -> col.trim().split(","))
            .flatMap(Arrays::stream)
            .map(String::trim)
            .map(gene -> {
                Ortholog ortholog = new Ortholog();
                ortholog.group = group;
                ortholog.gene = gene;
                return ortholog;
            })
            .toList();
    }
}
