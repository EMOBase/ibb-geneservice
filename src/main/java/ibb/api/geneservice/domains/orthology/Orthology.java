package ibb.api.geneservice.domains.orthology;

import java.util.ArrayList;
import java.util.List;

import ibb.api.geneservice.es.ESDoc;

public class Orthology implements ESDoc {

    public static class Ortholog {
        public String species;
        public String gene;
    }

    public String group;
    public String source;
    public List<Ortholog> orthologs = new ArrayList<>();

    @Override
    public String _id() {
        return source + ":" + group;
    }

    public void addOrtholog(String species, String gene) {
        Ortholog ortholog = new Ortholog();
        ortholog.species = species;
        ortholog.gene = gene;
        orthologs.add(ortholog);
    }
}
