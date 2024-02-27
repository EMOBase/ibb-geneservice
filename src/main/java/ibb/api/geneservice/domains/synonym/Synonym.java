package ibb.api.geneservice.domains.synonym;

import java.util.List;

import ibb.api.geneservice.es.ESDoc;

public class Synonym implements ESDoc {

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
    public String species;
    public Type type;
    public String value;

    public Synonym(String species, String gene, Type type, String value) {
        this.species = species;
        this.gene = gene;
        this.type = type;
        this.value = value;
    }

    @Override
    public String _id() {
        return String.join(":", List.of(species, type.name(), value));
    }
}
