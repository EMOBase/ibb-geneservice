package ibb.api.geneservice.domains.sequence;

import ibb.api.geneservice.es.ESDoc;

public class Sequence implements ESDoc {
    public String name;
    public String species;
    public String sequence;
    public SequenceType type;

    @Override
    public String _id() {
        return species + ":" + type + ":" + name;
    }
}
