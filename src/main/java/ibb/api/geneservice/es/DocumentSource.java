package ibb.api.geneservice.es;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;

public class DocumentSource<T> {
    public File file;
    public TextParser<T> parser;
    public String ingestPipeline;
    public List<String> tags = Collections.emptyList();

    public DocumentSource(File file, TextParser<T> parser) {
        this.file = file;
        this.parser = parser;
    }

    public DocumentSource<T> withIngestPipeline(String ingestPipeline) {
        this.ingestPipeline = ingestPipeline;
        return this;
    }

    public DocumentSource<T> withTags(List<String> tags) {
        this.tags = tags;
        return this;
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
