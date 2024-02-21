package ibb.api.geneservice.domains.synonym;

public class Synonym {

    public static enum Type {
        TRANSCRIPT,
        PROTEIN,
        OLD_IDS,
        NAME,
        SYMBOL,
        OTHER_NAMES,
        OTHER_SYMBOLS,
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
