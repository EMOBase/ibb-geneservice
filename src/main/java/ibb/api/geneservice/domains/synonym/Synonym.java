package ibb.api.geneservice.domains.synonym;

import java.util.List;

import ibb.api.geneservice.es.ESDoc;

public class Synonym implements ESDoc {

    public static enum Type {
        CURRENT_ID,
        NAME,
        SYMBOL,
        DSRNA,
        OLD_ID,
        TRANSCRIPT,
        PROTEIN,
        OTHER,
    }

    public String gene;
    public Type type;
    public String synonym;

    public Synonym(String gene, Type type, String synonym) {
        this.gene = gene;
        this.type = type;
        this.synonym = synonym;
    }

    @Override
    public String _id() {
        switch (type) {
            case CURRENT_ID:
            case NAME:
            case SYMBOL:
                // Allow only one synonym of these types
                return String.join(":", List.of(gene, type.name().toLowerCase()));
        
            default:   
                return String.join(":", List.of(gene, type.name().toLowerCase(), synonym));
        }
        
    }
}
