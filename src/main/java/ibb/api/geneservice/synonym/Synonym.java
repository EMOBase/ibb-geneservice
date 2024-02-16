package ibb.api.geneservice.synonym;

public class Synonym {

    public String gene;
    public String type;
    public String value;

    public Synonym(String gene, String type, String value) {
        this.gene = gene;
        this.type = type;
        this.value = value;
    }
}
