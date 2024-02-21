package ibb.api.geneservice.domains.sequence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.FastaParser;
import ibb.api.geneservice.parser.TextParser;

public class SequenceParser implements TextParser<Sequence> {

    private SequenceType type;

    public SequenceParser(SequenceType type) {
        this.type = type;
    }

    @Override
    public Stream<Sequence> parse(Path path) throws IOException {
        return new FastaParser().parse(path)
            .map(record -> {
                var sequence = new Sequence();
                sequence.name = record.header;
                sequence.sequence = record.sequence;
                sequence.type = type;
                return sequence;
            });
    }
}
