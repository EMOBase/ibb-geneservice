package ibb.api.geneservice.domains.orthology;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import ibb.api.geneservice.utils.Species;

public class OrthologyParser extends TextParser<Orthology> {

    private String source;
    private Species[] species;
    private final String delimiter = "\t";

    public OrthologyParser(String source) {
        this.source = source;
    }

    @Override
    public Stream<Orthology> parse(Path path) throws IOException {
        Stream<String> lines = parseText(path).filter(line -> !line.isBlank());
        var iterator = lines.iterator();
        species = Arrays.stream(iterator.next().split(delimiter))
            .skip(1)
            .map(Species::of)
            .toArray(Species[]::new);

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
            .map(this::parseLine)
            .onClose(lines::close);
    }
    
    private Orthology parseLine(String line) {
        String[] cols = line.split(delimiter);
        if (cols.length < 3) {
            throw new TextParserException(getLineNumber(), "Ortholog file must have at least 3 columns");
        }

        Orthology orthology = new Orthology();
        orthology.group = source + ":" + cols[0];

        for (int i = 1; i < cols.length; i++) {
            Species species = this.species[i - 1];
            List<String> genes = Arrays.stream(cols[i].trim().split(","))
                .map(String::trim)
                .filter(gene -> !gene.isBlank())
                .map(gene -> species.createGeneId(gene))
                .toList();
            orthology.orthologs.addAll(genes);
        }
        return orthology;
    }
}
