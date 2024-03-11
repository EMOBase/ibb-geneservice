package ibb.api.geneservice.domains.dsrna;

import ibb.api.geneservice.es.ESDoc;
import ibb.api.geneservice.utils.Species;

public class DsRNA implements ESDoc {
    public String id;
    public Species species;
    public String leftPrimer;
    public String rightPrimer;
    public String seq;

    @Override
    public String _id() {
        return species.createGeneId(id);
    }
}
