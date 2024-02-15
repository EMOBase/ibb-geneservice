package ibb.api.geneservice.parser;

public class FastaRecord {
    public String header;
    public String sequence;

    public FastaRecord(String header, String sequence) {
        this.header = header;
        this.sequence = sequence;
    }
}
