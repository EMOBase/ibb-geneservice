package ibb.api.geneservice.index;

import java.io.File;

import ibb.api.geneservice.parser.TextParser;

public class DocumentSource<T> {
    File file;
    TextParser<T> parser;

    public DocumentSource(File file, TextParser<T> parser) {
        this.file = file;
        this.parser = parser;
    }
}
