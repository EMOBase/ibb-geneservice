package ibb.api.geneservice.sequence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;

public final class FastaParser implements TextParser<FastaRecord> {

    /**
     * Parse a FASTA file into a stream of {@link FastaRecord} objects.
     * 
     * @param path the path to the FASTA file
     * @return a stream of {@link FastaRecord} objects
     * @throws IOException if an I/O error occurs
     * @throws TextParserException if the file is not a valid FASTA file
     * @apiNote This method must be used within a try-with-resources statement or similar control structure to ensure that the stream's open file is closed promptly after the stream's operations have completed.
     */
    public Stream<FastaRecord> parse(Path path) throws IOException {
        var stream = parseText(path);
        var lineIterator = stream.iterator();
        var fastaIterator = new FastaIterator(lineIterator);
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(fastaIterator, Spliterator.ORDERED),
            false
        ).onClose(stream::close);
    }

    private class FastaIterator implements Iterator<FastaRecord> {

        private Iterator<String> lineIterator;
        private String lastHeader;
        private FastaRecord current = null;
        private int lineNumber = 0;
    
        private FastaIterator(Iterator<String> lineIterator) {
            this.lineIterator = lineIterator;
        }
    
        @Override
        public boolean hasNext() {
            if (current == null) {
                current = getNextFastaRecord();
            }
            return current != null;
        }
    
        @Override
        public FastaRecord next() {
            FastaRecord next = current;
            current = null;
            if (next == null) {
                next = getNextFastaRecord();
                if (next == null) {
                    throw new NoSuchElementException();
                }
            }
            return next;
        }
    
        public FastaRecord getNextFastaRecord() {
            StringBuilder sequence = new StringBuilder();
            while (true) {
                if (!lineIterator.hasNext()) {
                    var fastaRecord = makeFastaRecord(lastHeader, sequence);
                    lastHeader = null;
                    return fastaRecord;
                }
                
                lineNumber ++;
                String line = lineIterator.next().trim();
                if (isEmptyLine(line)) {
                    continue;
                } else if (isHeaderLine(line)) {
                    var fastaRecord = makeFastaRecord(lastHeader, sequence);
                    lastHeader = line;
                    if (fastaRecord != null) {
                        return fastaRecord;
                    }
                } else {
                    if (lastHeader == null) {
                        throw new TextParserException(lineNumber, "Invalid FASTA format: sequence data found before header");
                    }
                    sequence.append(line);
                }
            }
        }
    
        private FastaRecord makeFastaRecord(String header, StringBuilder sequence) {
            if (header == null) {
                return null;
            }
            return new FastaRecord(parseHeader(header), sequence.toString());
        }
    
        private boolean isEmptyLine(String line) {
            return Objects.equals("", line);
        }
    
        private boolean isHeaderLine(String line) {
            return line != null && line.startsWith(">");
        }
    
        private String parseHeader(String line) {
            return line.substring(1).trim().split(" ")[0];
        }
    }
}
