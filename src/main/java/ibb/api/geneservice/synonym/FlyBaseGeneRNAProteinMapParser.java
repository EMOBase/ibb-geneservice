package ibb.api.geneservice.synonym;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;

public class FlyBaseGeneRNAProteinMapParser implements TextParser<Synonym> {

    AtomicInteger lineCount = new AtomicInteger(0);

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
            .flatMap(List::stream)
            .filter(s -> s.value != null && !s.value.isBlank());
    }

    private List<Synonym> parseLine(String line) {
        final String delimiter = "\t";

        String[] cols = line.split(delimiter);
        if (cols.length != 3) {
            throw new TextParserException(lineCount.get(), "FlyBase gene RNA protein map file must have 3 columns");
        }

        String gene = cols[0];
        String transcript = cols[1];
        String protein = cols[2];

        return List.of(
            new Synonym(gene, "transcript", transcript),
            new Synonym(gene, "protein", protein)
        );
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }
}
