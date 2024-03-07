package ibb.api.geneservice.parser;

public class TextParserException extends RuntimeException {
    public TextParserException(long lineNumber, String errorMessage) {
        super("Line " + lineNumber + ": " + errorMessage);
    }
}
