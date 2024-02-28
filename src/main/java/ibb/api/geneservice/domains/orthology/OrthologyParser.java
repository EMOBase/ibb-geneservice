package ibb.api.geneservice.domains.orthology;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ibb.api.geneservice.parser.TextParser;

public class OrthologyParser implements TextParser<Orthology> {

    private int priority;
    private String source;
    private String[] headers;
    private final String delimiter = "\t";

    public OrthologyParser(String source, int priority) {
        this.source = source;
        this.priority = priority;
    }

    public OrthologyParser(String source) {
        this(source, 999);
    }

    @Override
    public Stream<Orthology> parse(Path path) throws IOException {
        Stream<String> lines = parseText(path).filter(line -> !line.isBlank());
        var iterator = lines.iterator();
        headers = iterator.next().split(delimiter);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
            .map(this::parseLine)
            .onClose(lines::close);
    }
    
    private Orthology parseLine(String line) {
        String[] cols = line.split(delimiter);
        if (cols.length < 3) {
            throw new IllegalArgumentException("Ortholog file must have at least 3 columns");
        }

        Orthology orthology = new Orthology();
        orthology.group = cols[0];
        orthology.source = source;
        orthology.priority = priority;

        for (int i = 1; i < cols.length; i++) {
            for (String gene : cols[i].trim().split(",")) {
                gene = gene.trim();
                if (!gene.isBlank()) {
                    orthology.addOrtholog(headers[i], gene);
                }
            }
        }
        return orthology;
    }
}
