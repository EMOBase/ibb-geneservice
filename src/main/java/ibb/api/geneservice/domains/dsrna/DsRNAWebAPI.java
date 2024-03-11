package ibb.api.geneservice.domains.dsrna;

import java.util.List;

import org.jboss.resteasy.reactive.RestPath;

import ibb.api.geneservice.utils.Species;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/dsrna")
public class DsRNAWebAPI {
    
    @Inject
    DsRNAIndex index;

    @GET
    @Path("/{species}")
    public List<DsRNA> getByIds(
        @RestPath Species species,
        @RestPath @NotNull List<@NotBlank String> names
    ) {
        List<String> ids = names.stream().map(species::createGeneId).toList();
        return index.findByIds(ids);
    }
}
