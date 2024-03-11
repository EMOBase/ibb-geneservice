package ibb.api.geneservice.webapi;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import ibb.api.geneservice.webapi.SearchHandler.SearchResult;
import ibb.api.geneservice.webapi.legacy.DrosophilaGene;
import ibb.api.geneservice.webapi.legacy.GroupedOrthology;
import ibb.api.geneservice.webapi.legacy.SilencingSeq;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/")
public class WebAPI {
    
    @Inject
    SearchHandler searchHandler;

    @GET
    @Path("/search")
    public SearchResult search(@RestQuery @NotBlank String query) {
        return searchHandler.search(query);
    }

    @GET
    @Path("/search/_suggest")
    public List<String> suggest(@RestQuery @NotBlank String query) {
      	return searchHandler.suggest(query);
    }

    @GET
    @Path("/datasources/{source}/drosophila/genes")
    public List<GroupedOrthology> getOrthologyByDrosophilaGenes(@RestPath @NotBlank String source, @RestQuery @NotBlank String geneIds) {
        return null;
    }

    @GET
    @Path("/datasources/{source}/tribolium/genes")
    public List<GroupedOrthology> getOrthologyByTriboliumGenes(@RestPath @NotBlank String source, @RestQuery @NotBlank String geneIds) {
        return null;
    }

    @GET
    @Path("/silencingseqs")
    @Operation(summary = "Find a list of silencing sequences (iB fragments)")
    @Parameters({
        @Parameter(name = "geneIds",
                description = "Find sequences that match these genes (comma-separated). Should not be used together with param \"ids\"", 
                example = "TC000292,TC000021"),
        @Parameter(name = "ids",
                description = "Find sequences with these ids (comma-separated). Should not be used together with param \"geneIds\"",
                example = "iB_00061,iB_00010")
    })

    public List<SilencingSeq> getSilencingSeqs(
        @DefaultValue("") @QueryParam("geneIds") String geneIds,
        @DefaultValue("") @QueryParam("ids") String ids
) {
        Set<String> idSet = trimAndSplit(ids, HashSet::new);
        Set<String> geneIdSet = trimAndSplit(geneIds, HashSet::new);
        
        if (idSet.size() > 0 && geneIdSet.size() > 0) {
            throw new BadRequestException("The params \"ids\" and \"geneIds\" should not be used together");
        }

        return null;
    }

    @GET
    @Path("/drosophila/genes")
    @Operation(summary = "Get information of a list of drosophila genes")
    @Parameters({
        @Parameter(name = "ids", description = "Comma-separated list of Drosophila gene identifiers in format FBgn[0-9]{7}", example = "FBgn0000015,FBgn0000008"),
        @Parameter(name = "symbol"),
        @Parameter(name = "fullname"),
        @Parameter(name = "annotationId")
    })
    public List<DrosophilaGene> getDrosophilaGenes(
        @DefaultValue("") @QueryParam("ids") String ids,
        @QueryParam("symbol") String symbol,
        @QueryParam("fullname") String fullname,
        @QueryParam("annotationId") String annotationId
    ) {
        Set<String> idSet = trimAndSplit(ids, HashSet::new);
        
        long paramCnt = Stream.of(symbol, fullname, annotationId).filter(Objects::nonNull).count();
        if (paramCnt > 0 && idSet.size() > 0 || idSet.size() == 0 && paramCnt > 1) {
            throw new BadRequestException("Only one query param is allowed");
        }

        return null;
    }

    @GET
    @Path("/tribolium/genes")
    @Operation(summary = "Get information of a list of tribolium genes")
    @Parameters({
        @Parameter(name = "ids", description = "Comma-separated list of Tribolium gene identifiers in format TC[0-9]{6}}", example = "TC016177,TC001906")
    })
    public List<DrosophilaGene> getTriboliumGenes(@DefaultValue("") @QueryParam("ids") String ids) {
        Set<String> idSet = trimAndSplit(ids, HashSet::new);
        return null;
    }

    private static <C extends Collection<String>> C trimAndSplit(String str, Supplier<C> collectionFactory) {
        return Arrays.stream(str.trim().split(","))
                .map(String::trim)
                .filter(Predicate.not(""::equals))
                .collect(Collectors.toCollection(collectionFactory));
    }
}
