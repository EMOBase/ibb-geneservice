package ibb.api.geneservice.domains.genomic;

import org.jboss.resteasy.reactive.RestPath;

import ibb.api.geneservice.utils.Species;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;

@Path("/genomiclocation")
public class GenomicLocationWebAPI {
    
    @Inject
    GenomicLocationIndex genomicLocationIndex;

    @GET
    @Path("/{species}/{gene}")
    public GenomicLocation getGenomicLocation(@RestPath String species, @RestPath String gene) {
        String id;
        try {
            id = Species.of(species).createGeneId(gene);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException();
        }
        return genomicLocationIndex.findById(id).orElseThrow(NotFoundException::new);
    }
}
