package ibb.api.geneservice.search;

import java.util.List;

import org.jboss.resteasy.reactive.RestQuery;

import ibb.api.geneservice.domains.synonym.SynonymSuggestResult;
import ibb.api.geneservice.search.SearchHandler.SearchResult;
import ibb.api.geneservice.validator.ZeroOr;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/search")
public class SearchWebAPI {
    
    @Inject
    SearchHandler searchHandler;

    @GET
    public SearchResult search(@RestQuery @NotBlank String query) {
        return searchHandler.search(query);
    }

    @GET
    @Path("/_suggest")
    public SynonymSuggestResult suggest(
        @RestQuery @NotBlank String query,
        @RestQuery List<@NotBlank String> searchAfter) {
      	return searchHandler.suggest(query, searchAfter);
    }
}
