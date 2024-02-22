package ibb.api.geneservice.index;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;

public class DocumentSource<T> {
    public File file;
    public TextParser<T> parser;

    public DocumentSource(File file, TextParser<T> parser) {
        this.file = file;
        this.parser = parser;
    }

    /**
     * Open a stream to the file and parse its content.
     * @return a stream of parsed objects
     * @throws IOException if an I/O error occurs
     * @apiNote This method must be used within a try-with-resources statement or similar control structure to ensure that the stream's open file is closed promptly after the stream's operations have completed.
     */
    public Stream<T> stream() throws IOException {
        return parser.parse(file.toPath());
    }
}
