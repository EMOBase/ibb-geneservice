package ibb.api.geneservice.webapi.legacy;

import java.util.List;

public class GroupedOrthology {
    public static class Ortholog {
        public String source;
        public String gene;
    }
    public String gene;
    public List<Ortholog> orthologs;
}
