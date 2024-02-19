package ibb.api.geneservice.genomic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;

public class GenomicParser implements TextParser<Genomic> {

    /**
     * Parse a GFF3 file into a stream of {@link Genomic} objects.
     * @param path the path to the GFF3 file
     * @return a stream of {@link Genomic} objects
     * @throws IOException if an I/O error occurs
     * @throws TextParserException if the file is not a valid GFF3 file
     * @apiNote This method must be used within a try-with-resources statement or similar control structure to ensure that the stream's open file is closed promptly after the stream's operations have completed.
     */
    public Stream<Genomic> parse(Path path) throws IOException {
        var stream = new GFF3Parser().parse(path);
        var gff3RecordIterator = stream.iterator();
        var geneIterator = new GenomicIterator(gff3RecordIterator);
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(geneIterator, Spliterator.ORDERED),
            false
        ).onClose(stream::close);
    }
    private static class GenomicIterator implements Iterator<Genomic> {
        private Iterator<GFF3Record> gff3RecordIterator;
        private int lineNumber = 0;
        private List<GFF3Record> gff3Records = new ArrayList<>();
        private Genomic current = null;

        public GenomicIterator(Iterator<GFF3Record> gff3RecordIterator) {
            this.gff3RecordIterator = gff3RecordIterator;
        }

        private Genomic getNextGene() {
            while (true) {
                if (!gff3RecordIterator.hasNext()) {
                    Genomic gene = makeGene(gff3Records);
                    gff3Records.clear();
                    return gene;
                }
                lineNumber++;
                GFF3Record gff3Record = gff3RecordIterator.next();
                switch (gff3Record.getType()) {
                    case "gene":
                        Genomic gene = makeGene(gff3Records);
                        gff3Records.clear();
                        gff3Records.add(gff3Record);
                        if (gene != null) {
                            return gene;
                        }
                    default:
                        gff3Records.add(gff3Record);
                        break;
                }
            }
        }

        /**
         * Make a {@link Genomic} object from a block of {@link GFF3Record} objects.
         * In the GFF3 format, a gene is represented by a block of records, where the first record is a "gene" record and the subsequent records are "mRNA", "CDS", etc. records that are children of the gene record.
         * @param gff3Records the list of {@link GFF3Record} objects
         * @return a {@link Genomic} object
         */
        private Genomic makeGene(List<GFF3Record> gff3Records) {
            Genomic gene = null;
            String geneLocalId = null;
            Set<String> mRNALocalIds = new HashSet<>();
            for (GFF3Record record: gff3Records) {
                switch (record.getType()) {
                    case "gene":
                        geneLocalId = record.getId();

                        gene = new Genomic();
                        gene.id = getGeneId(record);
                        gene.name = record.getAttributeFirstValue("description");
                        gene.referenceSeq = record.getSeqId();
                        gene.start = record.getStart();
                        gene.end = record.getEnd();
                        gene.strand = record.getStrand().getSymbol();
                        break;
                
                    case "mRNA":
                        mRNALocalIds.add(record.getId());
                        if (gene == null || !Objects.equals(geneLocalId, record.getParentId())) {
                            throw new TextParserException(lineNumber, "mRNA must have a gene parent declared before it");
                        }

                        String mRNAXrefId = record.getAttributeFirstValue("transcript_id");
                        if (mRNAXrefId != null) {
                            gene.mRNAnames.add(mRNAXrefId);
                        }
                        String proteinXrefId = record.getAttributeFirstValue("protein_id");
                        if (proteinXrefId != null) {
                            gene.proteinNames.add(proteinXrefId);
                        }
                        break;
                    case "CDS":
                        if (!mRNALocalIds.contains(record.getParentId())) {
                            throw new TextParserException(lineNumber, "CDS must have an mRNA parent declared before it");
                        }
                        String CDSXrefId = record.getAttributeFirstValue("protein_id");
                        if (CDSXrefId != null) {
                            gene.proteinNames.add(CDSXrefId);
                        }
                        break;
                        
                    default:
                        break;
                }
            }
            return gene;
        }

        @Override
        public boolean hasNext() {
            if (current == null) {
                current = getNextGene();
            }
            return current != null;
        }

        @Override
        public Genomic next() {
            Genomic next = current;
            current = null;
            if (next == null) {
                next = getNextGene();
                if (next == null) {
                    throw new NoSuchElementException();
                }
            }
            return next;
        }

        private String getGeneId(GFF3Record record) {
            String locusTag = record.getAttributeFirstValue("locus_tag");
            if (locusTag != null) {
                Pattern pattern = Pattern.compile("TC[0-9]{6}");
                Matcher matcher = pattern.matcher(locusTag);
                if (matcher.find()) {
                    return matcher.group();
                }
            }
            for (String value: record.getAttribute("Dbxref")) {
                String[] pair = value.split(":");
                String db = pair[0];
                String xrefId = pair[1];
                if (Objects.equals("GeneID", db)) {
                    return xrefId;
                }
            }
            throw new TextParserException(lineNumber, "Can't find gene xref id");
        }
    }
}
