package ibb.api.geneservice.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Gene {

    public static class Alias {
        public String type;
        public String value;
    }

    public String id;
    public String name;
    public List<Alias> aliases;

    public String referenceSeq;
    public Integer start;
    public Integer end;
    public String strand;
    public List<String> dsRNAIds;
    public List<String> orthologGroups;

    public Set<String> CDSNames = new LinkedHashSet<>();
    public Set<String> mRNAnames = new LinkedHashSet<>();
    public Set<String> proteinNames = new LinkedHashSet<>();
}
