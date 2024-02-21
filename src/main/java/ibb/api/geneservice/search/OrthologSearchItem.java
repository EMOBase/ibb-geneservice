package ibb.api.geneservice.search;

import java.util.List;

import ibb.api.geneservice.domains.synonym.Synonym;

public class OrthologSearchItem {

    public static class Ortholog {
        public String gene;
        public List<Synonym> synonyms;
    }

    public String source;
    public String group;
    public List<Ortholog> orthologs;
}
