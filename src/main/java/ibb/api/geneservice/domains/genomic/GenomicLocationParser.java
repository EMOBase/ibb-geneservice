package ibb.api.geneservice.domains.genomic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import ibb.api.geneservice.parser.gff3.GFF3GeneIDFinder;
import ibb.api.geneservice.parser.gff3.GFF3Parser;
import ibb.api.geneservice.utils.Species;

public class GenomicLocationParser extends TextParser<GenomicLocation> {

    private GFF3GeneIDFinder gff3GeneIDFinder;
    private Species species;

    public GenomicLocationParser(Species species) {
        this.species = species;
        if (Objects.equals(species, Species.of("Tcas"))) {
            gff3GeneIDFinder = GFF3GeneIDFinder.byTCLocusTag();
        } else if (Objects.equals(species, Species.of("Lyst"))) {
            gff3GeneIDFinder = GFF3GeneIDFinder.byLocusTag();
        } else {
            gff3GeneIDFinder = GFF3GeneIDFinder.byNCBIGeneID();
        }
    }

    @Override
    public Stream<GenomicLocation> parse(Path path) throws IOException {
        var gff3Parser = new GFF3Parser();
        return gff3Parser.parse(path)
            .filter(gff3Record -> Objects.equals("gene", gff3Record.getType()))
            .map(record -> {
                GenomicLocation loc = new GenomicLocation();
                String gene = gff3GeneIDFinder.findGeneId(record).map(id -> id.current).orElseThrow(
                    () -> new TextParserException(gff3Parser.getLineNumber(), "Can't find gene xref id")
                );
                loc.gene = species.createGeneId(gene);
                loc.referenceSeq = record.getSeqId();
                loc.start = record.getStart();
                loc.end = record.getEnd();
                loc.strand = record.getStrand().getSymbol();
                return loc;
            });
    }
}
