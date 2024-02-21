package ibb.api.geneservice.domains.synonym;

public class Synonym {

    public static enum Type {
        TRANSCRIPT,
        PROTEIN,
        OLD_ID,
        NAME,
        SYMBOL,
        OTHER_NAME,
        OTHER_SYMBOL,
    }

    public String gene;
    public Type type;
    public String value;

    public Synonym(String gene, Type type, String value) {
        this.gene = gene;
        this.type = type;
        this.value = value;
    }
}
