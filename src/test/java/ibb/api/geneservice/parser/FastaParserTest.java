package ibb.api.geneservice.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FastaParserTest {
    private Path testDataDirectory = Path.of("src", "test", "resources", "fasta");

    @ParameterizedTest
    @ValueSource(strings = { "iBB_OGS3_mRNA_3samples.fasta", "iBB_OGS3_mRNA_3samples_compressed.fasta.gz" })
    public void testIBBOGS3MRNA3Samples(String fileName) throws Exception {
        Path path = testDataDirectory.resolve(fileName);
        try (var stream = new FastaParser().parse(path)) {
            FastaRecord[] records = stream.toArray(FastaRecord[]::new);
            assertEquals(3, records.length);
            testEqualsTC016174RA(records[0]);
        }
    }

    private void testEqualsTC016174RA(FastaRecord record) {
        assertEquals("gnl|WGS:AAJJ|TC016174-RA", record.header);
        assertNotNull(record.sequence);
        assertEquals(1404, record.sequence.length());
        String sequenceFirst10 = record.sequence.substring(0, 10);
        String sequenceLast10 = record.sequence.substring(record.sequence.length() - 10);
        assertEquals("atggacagta", sequenceFirst10);
        assertEquals("tgaggtctga", sequenceLast10);
    }
}
