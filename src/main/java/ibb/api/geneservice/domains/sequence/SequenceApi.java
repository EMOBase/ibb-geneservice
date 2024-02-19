package ibb.api.geneservice.domains.sequence;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/sequences")
public class SequenceApi {
    @Inject
    SequenceIndex sequenceIndex;

    @GET
    @Path("/{species}/{type}/{id}")
    public String getSequences() {
        return "Hello";
    }
}
