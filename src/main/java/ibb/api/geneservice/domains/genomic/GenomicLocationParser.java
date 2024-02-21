package ibb.api.geneservice.domains.genomic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import ibb.api.geneservice.parser.GFF3GeneIDFinder;
import ibb.api.geneservice.parser.GFF3Parser;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.TextParserException;

public class GenomicLocationParser implements TextParser<GenomicLocation> {


    private GFF3GeneIDFinder gff3GeneIDFinder;

    public GenomicLocationParser() {
        this.gff3GeneIDFinder = GFF3GeneIDFinder.byNCBIGeneID();
    }

    public GenomicLocationParser(GFF3GeneIDFinder geneIDFinder) {
        this.gff3GeneIDFinder = geneIDFinder;
    }

    @Override
    public Stream<GenomicLocation> parse(Path path) throws IOException {
        var gff3Parser = new GFF3Parser();
        return gff3Parser.parse(path)
            .filter(gff3Record -> Objects.equals("gene", gff3Record.getType()))
            .map(record -> {
                GenomicLocation loc = new GenomicLocation();
                loc.id = gff3GeneIDFinder.findGeneId(record).map(id -> id.current).orElseThrow(
                    () -> new TextParserException(gff3Parser.getLineNumber(), "Can't find gene xref id")
                );
                loc.referenceSeq = record.getSeqId();
                loc.start = record.getStart();
                loc.end = record.getEnd();
                loc.strand = record.getStrand().getSymbol();
                return loc;
            });
    }
}
