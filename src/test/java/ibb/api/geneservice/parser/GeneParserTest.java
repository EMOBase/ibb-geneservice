package ibb.api.geneservice.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import ibb.api.geneservice.genomic.GFF3Record;
import ibb.api.geneservice.genomic.Genomic;
import ibb.api.geneservice.genomic.GenomicParser;

public class GeneParserTest {
    private Path testDataDirectory = Path.of("src", "test", "resources", "gff");

    @Test
    public void testIBBOGS3Head100() throws Exception {
        Path path = testDataDirectory.resolve("iBB_OGS3_head100.gff");
        try (var stream = GenomicParser.parse(path)) {
            Genomic[] genes = stream.toArray(Genomic[]::new);
            assertEquals(17, genes.length);
            Genomic gene = genes[0];

            assertEquals("TC016174", gene.id);

            assertEquals(70347, gene.start);
            assertEquals(73823, gene.end);
            assertEquals("NW_015450206.1", gene.referenceSeq);
            assertEquals(GFF3Record.Strand.REVERSE.getSymbol(), gene.strand);

            assertTrue(gene.mRNAnames.contains("gnl|WGS:AAJJ|TC016174-RA"));
            assertTrue(gene.proteinNames.contains("gnl|WGS:AAJJ|TC016174-PA|gb|EFA12439"));
        }
    }
}
