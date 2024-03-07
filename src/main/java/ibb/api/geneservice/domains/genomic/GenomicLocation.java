package ibb.api.geneservice.domains.genomic;

import ibb.api.geneservice.es.ESDoc;

public class GenomicLocation implements ESDoc {
    public String gene;
    public String referenceSeq;
    public Integer start;
    public Integer end;
    public String strand;

    @Override
    public String _id() {
        return gene;
    }
}
