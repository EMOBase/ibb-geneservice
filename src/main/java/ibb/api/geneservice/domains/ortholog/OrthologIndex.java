package ibb.api.geneservice.domains.ortholog;

import ibb.api.geneservice.es.ESIndex;

public class OrthologIndex extends ESIndex<Ortholog> {

    public OrthologIndex(String source) {
        super("orthologs-" + source);
    }
}
