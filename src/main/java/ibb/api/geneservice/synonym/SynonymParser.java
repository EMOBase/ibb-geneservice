package ibb.api.geneservice.synonym;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface SynonymParser {
    Stream<Synonym> parse(Path path) throws IOException;
}
