package ibb.api.geneservice.domains.orthology;

import java.util.ArrayList;
import java.util.List;

import ibb.api.geneservice.es.ESDoc;

public class Orthology implements ESDoc {

    public String group;
    public List<String> orthologs = new ArrayList<>();

    @Override
    public String _id() {
        return group;
    }
}
