package ibb.api.geneservice.domains.genomic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;
import ibb.api.geneservice.parser.gff3.GFF3GeneIDFinder;
import ibb.api.geneservice.parser.gff3.GFF3Parser;

public class GenomicLocationParser implements TextParser<GenomicLocation> {

    private GFF3GeneIDFinder gff3GeneIDFinder;
    private String species;

    public GenomicLocationParser(String species) {
        this.species = species;
        if (Objects.equals("Tcas", species)) {
            gff3GeneIDFinder = GFF3GeneIDFinder.byTCLocusTag();
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
                loc.gene = gff3GeneIDFinder.findGeneId(record).map(id -> id.current).orElseThrow(
                    () -> new TextParserException(gff3Parser.getLineNumber(), "Can't find gene xref id")
                );
                loc.species = species;
                loc.referenceSeq = record.getSeqId();
                loc.start = record.getStart();
                loc.end = record.getEnd();
                loc.strand = record.getStrand().getSymbol();
                return loc;
            });
    }
}
