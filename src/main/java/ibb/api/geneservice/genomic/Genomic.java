package ibb.api.geneservice.genomic;

import java.util.LinkedHashSet;
import java.util.Set;

public class Genomic {
    public String id;
    public String name;

    public String referenceSeq;
    public Integer start;
    public Integer end;
    public String strand;

    public Set<String> CDSNames = new LinkedHashSet<>();
    public Set<String> mRNAnames = new LinkedHashSet<>();
    public Set<String> proteinNames = new LinkedHashSet<>();
}
