package ibb.api.geneservice.search;

import java.util.List;

public class OrthologySearchItem {

    public static class Synonym {
        public String synonym;
        public String type;
    }

    public static class Ortholog {
        public String gene;
        public String species;
        public List<Synonym> synonyms;
    }

    public String source;
    public String group;
    public List<Ortholog> orthologs;
}
