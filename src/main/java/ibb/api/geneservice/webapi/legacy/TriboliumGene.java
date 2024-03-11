package ibb.api.geneservice.webapi.legacy;

import java.util.List;

public class TriboliumGene {
    public static class Sequence {
        public String id;
        public String seq;
    }

    public String id;
    public int start;
    public int end;
    public String strand;
    public String seqname;
    public List<Sequence> mRNAs;
    public List<Sequence> proteins;
    public List<Sequence> CDS;
}
