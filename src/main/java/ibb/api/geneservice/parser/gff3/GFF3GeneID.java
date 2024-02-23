package ibb.api.geneservice.parser.gff3;

import java.util.List;

public class GFF3GeneID {
    public String current;
    public List<String> previous;

    public GFF3GeneID(String current, List<String> previous) {
        this.current = current;
        this.previous = previous;
    }
}
