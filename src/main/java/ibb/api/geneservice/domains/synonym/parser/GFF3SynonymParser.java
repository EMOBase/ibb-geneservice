package ibb.api.geneservice.domains.synonym.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ibb.api.geneservice.domains.genomic.GenomicLocation;
import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import ibb.api.geneservice.parser.gff3.GFF3GeneIDFinder;
import ibb.api.geneservice.parser.gff3.GFF3Parser;
import ibb.api.geneservice.parser.gff3.GFF3Record;
import ibb.api.geneservice.utils.Species;

public class GFF3SynonymParser extends TextParser<Synonym> {

    private GFF3GeneIDFinder gff3GeneIDFinder;
    private Species species;

    public GFF3SynonymParser(Species species) {
        this.species = species;
        if (Objects.equals(species, Species.of("Tcas"))) {
            gff3GeneIDFinder = GFF3GeneIDFinder.byTCLocusTag();
        } else if (Objects.equals(species, Species.of("Lyst"))) {
            gff3GeneIDFinder = GFF3GeneIDFinder.byLocusTag();
        } else {
            gff3GeneIDFinder = GFF3GeneIDFinder.byNCBIGeneID();
        }
    }

    /**
     * Parse a GFF3 file into a stream of {@link GenomicLocation} objects.
     * @param path the path to the GFF3 file
     * @return a stream of {@link GenomicLocation} objects
     * @throws IOException if an I/O error occurs
     * @throws TextParserException if the file is not a valid GFF3 file
     * @apiNote This method must be used within a try-with-resources statement or similar control structure to ensure that the stream's open file is closed promptly after the stream's operations have completed.
     */
    public Stream<Synonym> parse(Path path) throws IOException {
        var gff3Parser = new GFF3Parser();
        var stream = gff3Parser.parse(path);
        var gff3RecordIterator = stream.iterator();
        var synonymIterator = new SynonymIterator(gff3RecordIterator, gff3Parser, this);
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(synonymIterator, Spliterator.ORDERED),
            false
        ).flatMap(List::stream).onClose(stream::close);
    }

    private static class SynonymIterator implements Iterator<List<Synonym>> {
        private GFF3Parser gff3Parser;
        private Iterator<GFF3Record> gff3RecordIterator;
        private GFF3SynonymParser synonymParser;
        private List<GFF3Record> gff3Records = new ArrayList<>();
        private List<Synonym> current = null;

        public SynonymIterator(Iterator<GFF3Record> gff3RecordIterator, GFF3Parser gff3Parser, GFF3SynonymParser synonymParser) {
            this.gff3Parser = gff3Parser;
            this.gff3RecordIterator = gff3RecordIterator;
            this.synonymParser = synonymParser;
        }

        private List<Synonym> getNextSynonyms() {
            while (true) {
                if (!gff3RecordIterator.hasNext()) {
                    if (gff3Records.isEmpty()) {
                        return null;
                    }
                    var synonyms = makeSynonyms(gff3Records);
                    gff3Records.clear();
                    return synonyms;
                }
                GFF3Record gff3Record = gff3RecordIterator.next();
                switch (gff3Record.getType()) {
                    case "gene":
                        if (gff3Records.isEmpty()) {
                            gff3Records.add(gff3Record);
                        } else {
                            var synonyms = makeSynonyms(gff3Records);
                            gff3Records.clear();
                            gff3Records.add(gff3Record);
                            return synonyms;
                        }
                    default:
                        if (!gff3Records.isEmpty()) {
                            gff3Records.add(gff3Record);
                        }
                        break;
                }
            }
        }

        /**
         * Make {@link Synonym} objects from a block of {@link GFF3Record} objects.
         * In the GFF3 format, a gene is represented by a block of records, where the first record is a "gene" record and the subsequent records are "mRNA", "CDS", etc. records that are children of the gene record.
         * @param gff3Records the list of {@link GFF3Record} objects
         * @return a list of {@link Synonym} objects
         */
        private List<Synonym> makeSynonyms(List<GFF3Record> gff3Records) {
            List<Synonym> synonyms = new ArrayList<>();
            
            GFF3Record geneRecord = gff3Records.get(0);
            if (!"gene".equals(geneRecord.getType())) {
                throw new IllegalStateException("First record must be a gene record");
            }

            var geneXrefIdGroup = synonymParser.gff3GeneIDFinder.findGeneId(geneRecord).orElseThrow(
                () -> new TextParserException(gff3Parser.getLineNumber(), "Can't find gene xref id")
            );

            Species species = synonymParser.species;
            String gene = species.createGeneId(geneXrefIdGroup.current);
            synonyms.add(new Synonym(gene, Synonym.Type.CURRENT_ID, geneXrefIdGroup.current));

            geneXrefIdGroup.previous.stream()
                .map((previous) -> new Synonym(gene, Synonym.Type.OLD_ID, previous))
                .forEach(synonyms::add);

            geneRecord.getAttributeFirstValueOptional("description")
                .map(name -> new Synonym(gene, Synonym.Type.NAME, name))
                .ifPresent(synonyms::add); 

            String geneLocalId = geneRecord.getId();
            Set<String> mRNALocalIds = new HashSet<>();
            Set<String> mRNAXrefIds = new HashSet<>();
            Set<String> proteinXrefIds = new HashSet<>();

            gff3Records.stream().skip(1).forEach(record -> {
                switch (record.getType()) {
                    case "mRNA":
                        mRNALocalIds.add(record.getId());
                        if (geneLocalId == null || !Objects.equals(geneLocalId, record.getParentId())) {
                            throw new TextParserException(gff3Parser.getLineNumber(), "mRNA must have a gene parent declared before it");
                        }

                        record.getAttributeFirstValueOptional("transcript_id").ifPresent(mRNAXrefIds::add);
                        record.getAttributeFirstValueOptional("protein_id").ifPresent(allIds -> {
                            parseProteinId(allIds).stream().forEach(proteinXrefIds::add);
                        });
                        break;
                    case "CDS":
                        if (!mRNALocalIds.contains(record.getParentId()) && !Objects.equals(geneLocalId, record.getParentId())) {
                            throw new TextParserException(gff3Parser.getLineNumber(), "CDS must have an mRNA or gene parent declared before it");
                        }
                        record.getAttributeFirstValueOptional("protein_id").ifPresent(allIds -> {
                            parseProteinId(allIds).stream().forEach(proteinXrefIds::add);
                        });
                        break;
                        
                    default:
                        break;
                }
            });
            mRNAXrefIds.stream()
                .map((mRNAXrefId) -> new Synonym(gene, Synonym.Type.TRANSCRIPT, mRNAXrefId))
                .forEach(synonyms::add);

            proteinXrefIds.stream()
                .map((proteinXrefId) -> new Synonym(gene, Synonym.Type.PROTEIN, proteinXrefId))
                .forEach(synonyms::add);
            return synonyms;
        }

        private List<String> parseProteinId(String input) {
            var vals = new ArrayDeque<>(Arrays.asList(input.split("\\|")));
            List<String> proteinIds = new ArrayList<>();
            List<String> remains = new ArrayList<>();

            while (true) {
                String val = vals.poll();
                if (val == null) {
                    break;
                } else if (val.equals("gnl")) {
                    proteinIds.add(val + "|" + vals.poll() + "|" + vals.poll());
                } else if (val.equals("gb")) {
                    proteinIds.add(val + "|" + vals.poll());
                } else {
                    remains.add(val);
                }
            }

            if (!remains.isEmpty()) {
                proteinIds.add(String.join("|", remains));
            }
            return proteinIds;
        }

        @Override
        public boolean hasNext() {
            if (current == null) {
                current = getNextSynonyms();
            }
            return current != null;
        }

        @Override
        public List<Synonym> next() {
            List<Synonym> next = current;
            current = null;
            if (next == null) {
                next = getNextSynonyms();
                if (next == null) {
                    throw new NoSuchElementException();
                }
            }
            return next;
        }
    }
}
