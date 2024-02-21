package ibb.api.geneservice.domains.ortholog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;

public class OrthologParser implements TextParser<Ortholog> {

    private String source;

    public OrthologParser(String source) {
        this.source = source;
    }

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
            .map(value -> {
                Ortholog ortholog = new Ortholog();
                ortholog.group = source + "-" + group;
                ortholog.ortholog = value;
                return ortholog;
            })
            .toList();
    }
}
