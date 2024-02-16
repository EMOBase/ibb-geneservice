package ibb.api.geneservice.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ibb.api.geneservice.genomic.GFF3Parser;
import ibb.api.geneservice.genomic.GFF3Record;

public class GFF3ParserTest {
    
    private Path testDataDirectory = Path.of("src", "test", "resources", "gff");

    @ParameterizedTest
    @ValueSource(strings = { "iBB_OGS3_head100.gff", "iBB_OGS3_head100_compressed.gff.gz" })
    public void testIBBOGS3Head100(String fileName) throws Exception {
        Path path = testDataDirectory.resolve(fileName);
        try (var stream = GFF3Parser.parse(path)) {
            GFF3Record[] records = stream.toArray(GFF3Record[]::new);
            assertEquals(100, records.length);
            testEqualsGene0(records[0]);
        }
    }

    @Test
    public void testIBBOGS3Head2Emptylines() throws Exception {
        Path path = testDataDirectory.resolve("iBB_OGS3_head2_emptylines.gff");
        try (var stream = GFF3Parser.parse(path)) {
            GFF3Record[] records = stream.toArray(GFF3Record[]::new);
            assertEquals(2, records.length);
            testEqualsGene0(records[0]);
        }
    }

    @Test
    public void test8Columns() throws Exception {
        Path path = testDataDirectory.resolve("8columns.gff");
        try (var stream = GFF3Parser.parse(path)) {
            Exception exception = assertThrows(TextParserException.class, () -> stream.toArray(GFF3Record[]::new));
            assertEquals("Line 1: Must have at least 9 columns", exception.getMessage());
        }
    }

    @Test
    public void test10Columns() throws Exception {
        Path path = testDataDirectory.resolve("10columns.gff");
        try (var stream = GFF3Parser.parse(path)) {
            GFF3Record[] records = stream.toArray(GFF3Record[]::new);
            assertEquals(1, records.length);
            testEqualsGene0(records[0]);
        }
    }

    private void testEqualsGene0(GFF3Record record) {
        assertEquals("NW_015450206.1", record.getSeqId());
        assertEquals("GenBank", record.getSource());
        assertEquals("gene", record.getType());
        assertEquals(70347, record.getStart());
        assertEquals(73823, record.getEnd());
        assertEquals(0, record.getScore());
        assertEquals(GFF3Record.Strand.REVERSE, record.getStrand());
        assertNull(record.getPhase());
        assertEquals("gene0", record.getId());
        assertEquals("1/1", record.getAttributeFirstValue("part"));
    }
}
