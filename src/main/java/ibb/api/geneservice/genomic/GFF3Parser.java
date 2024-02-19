package ibb.api.geneservice.genomic;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;

/**
 * GFF3 specification: http://gmod.org/wiki/GFF3
 */
public final class GFF3Parser implements TextParser<GFF3Record> {

    /** 
     * Parse a GFF3 file into a stream of {@link GFF3Record} objects.
     * @param path the path to the GFF3 file
     * @return a stream of {@link GFF3Record} objects
     * @throws IOException if an I/O error occurs
     * @throws TextParserException if the file is not a valid GFF3 file
     * @apiNote This method must be used within a try-with-resources statement or similar control structure to ensure that the stream's open file is closed promptly after the stream's operations have completed.
     */
    public Stream<GFF3Record> parse(Path path) throws IOException {
        var parser = new GFF3Parser();
        return parseText(path)
            .map(line -> {
                parser.lineNumber.incrementAndGet();
                return line;
            })
            .filter(line -> !parser.isHeaderLine(line))
            .filter(line -> !parser.isEmptyLine(line))
            .map(parser::parseGFF3Line);
    }

    private AtomicInteger lineNumber = new AtomicInteger(0);

    public GFF3Record parseGFF3Line(String line) {
        final String delimiter = "\t";

        String[] cols = Arrays.stream(line.split(delimiter))
            .map(String::trim)
            .map(col -> isEmptyValue(col) ? null : col)
            .toArray(String[]::new);

        if (cols.length < 9) {
            throw new TextParserException(lineNumber.get(), "Must have at least 9 columns");
        }

        String seqId = cols[0];
        String source = cols[1];
        String type = cols[2];
        String start = cols[3];
        String end = cols[4];
        String score = cols[5];
        String strand = cols[6];
        String phase = cols[7];
        String attributeString = cols[8];

        GFF3Record.Builder builder = new GFF3Record.Builder()
            .setSeqId(Optional.ofNullable(seqId)
                .map(this::decodeValue)
                .orElseThrow(() -> new TextParserException(lineNumber.get(), "SeqId is missing")))
            .setSource(source)
            .setType(Optional.ofNullable(type)
                .orElseThrow(() -> new TextParserException(lineNumber.get(), "Type is missing")))
            .setStart(Optional.ofNullable(start)
                .map(Integer::parseInt)
                .orElseThrow(() -> new TextParserException(lineNumber.get(), "Start position is missing")))
            .setEnd(Optional.ofNullable(end)
                .map(Integer::parseInt)
                .orElseThrow(() -> new TextParserException(lineNumber.get(), "End position is missing")))
            .setScore(Optional.ofNullable(score)
                .map(Float::parseFloat)
                .orElse(null))
            .setStrand(Optional.ofNullable(strand)
                .map(GFF3Record.Strand::getBySymbol)
                .orElse(null))
            .setPhase(Optional.ofNullable(phase)
                .map(Integer::parseInt)
                .orElse(null));

        if (attributeString != null) {
            parseAttributeColumn(attributeString)
                .forEach(pair -> builder.addAttribute(pair.getKey(), pair.getValue()));
        }
        return builder.build();
    }

    private Stream<Map.Entry<String, List<String>>> parseAttributeColumn(String attributeString) {
        final String attributeDelimiter = ";";
        final String valueDelimiter = ",";
        final String keyValueSeparator = "=";

        return Arrays.stream(attributeString.split(attributeDelimiter))
            .map(String::trim)
            .map(keyValueString -> keyValueString.split(keyValueSeparator, 2))
            .filter(pair -> pair.length == 2)
            .filter(pair -> !isEmptyValue(pair[1]))
            .map(pair -> Map.entry(
                pair[0],
                Arrays.stream(pair[1].split(valueDelimiter))
                    .filter(value -> !isEmptyValue(value))
                    .map(this::decodeValue)
                    .toList()
            ));
    }

    private String decodeValue(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private boolean isEmptyValue(String value) {
        return Objects.equals("", value) || Objects.equals(".", value);
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#");
    }

    private boolean isEmptyLine(String line) {
        return Objects.equals("", line);
    }
}
