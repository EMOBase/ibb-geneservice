package ibb.api.geneservice.domains.sequence;

public class FastaRecord {
    public String header;
    public String sequence;

    public FastaRecord(String header, String sequence) {
        this.header = header;
        this.sequence = sequence;
    }
}
