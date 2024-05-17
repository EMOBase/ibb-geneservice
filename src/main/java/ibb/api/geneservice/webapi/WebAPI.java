package ibb.api.geneservice.webapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import ibb.api.geneservice.domains.dsrna.DsRNA;
import ibb.api.geneservice.domains.dsrna.DsRNAIndex;
import ibb.api.geneservice.domains.genomic.GenomicLocationIndex;
import ibb.api.geneservice.domains.orthology.Orthology;
import ibb.api.geneservice.domains.orthology.OrthologyIndex;
import ibb.api.geneservice.domains.sequence.Sequence;
import ibb.api.geneservice.domains.sequence.SequenceIndex;
import ibb.api.geneservice.domains.sequence.SequenceType;
import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.utils.Species;
import ibb.api.geneservice.webapi.SearchHandler.SearchResult;
import ibb.api.geneservice.webapi.legacy.DrosophilaGene;
import ibb.api.geneservice.webapi.legacy.GroupedOrthology;
import ibb.api.geneservice.webapi.legacy.SilencingSeq;
import ibb.api.geneservice.webapi.legacy.TriboliumGene;
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

    @Inject
    OrthologyIndex orthologyIndex;

    @Inject
    SynonymIndex synonymIndex;

    @Inject
    DsRNAIndex dsRNAIndex;

    @Inject
    GenomicLocationIndex genomicLocationIndex;

    @Inject
    SequenceIndex sequenceIndex;

    @ConfigProperty(name = "geneservice.main-species")
    Species mainSpecies;

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
        Species species = Species.of("Dmel");
        List<String> queries = trimAndSplit(geneIds, ArrayList::new);
        return getOrthology(species, source, queries);
    }

    @GET
    @Path("/datasources/{source}/tribolium/genes")
    public List<GroupedOrthology> getOrthologyByTriboliumGenes(@RestPath @NotBlank String source, @RestQuery @NotBlank String geneIds) {
        Species species = Species.of("Tcas");
        List<String> queries = trimAndSplit(geneIds, ArrayList::new);
        return getOrthology(species, source, queries);
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
        } else if (idSet.size() > 0) {
            return listSilencingSeqsByIBnumbers(idSet.stream().toList());
        } else if (geneIdSet.size() > 0) {
            return listSilencingSeqsByGeneIds(geneIdSet.stream().toList());
        } else {
            return Collections.emptyList();
        }
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

        Species species = Species.of("Dmel");
        List<Synonym> synonyms;
        if (idSet.size() > 0) {
            synonyms = synonymIndex.findBySynonyms(idSet.stream().toList())
                .stream()
                .filter(s -> s.type == Synonym.Type.CURRENT_ID || s.type == Synonym.Type.OLD_ID)
                .toList();
        } else if (symbol != null) {
            synonyms = synonymIndex.findBySynonym(symbol)
                .stream()
                .filter(s -> s.type == Synonym.Type.SYMBOL)
                .toList();
        } else if (fullname != null) {
            synonyms = synonymIndex.findBySynonym(fullname)
                .stream()
                .filter(s -> s.type == Synonym.Type.NAME)
                .toList();
        } else if (annotationId != null) {
            synonyms = synonymIndex.findBySynonym(annotationId)
                .stream()
                .filter(s -> s.type == Synonym.Type.OTHER && s.synonym.startsWith("CG"))
                .toList();
        } else {
            synonyms = Collections.emptyList();
        }

        List<String> genes = synonyms.stream()
            .map(s -> s.gene)
            .filter(gene -> species.isGeneFromSpecies(gene))
            .toList();
        
        Map<String, List<Synonym>> allSynonyms = synonymIndex.findByGenes(genes)
            .stream()
            .collect(Collectors.groupingBy(s -> s.gene));

        List<DrosophilaGene> drosophilaGenes = new ArrayList<>();
        for (var entry: allSynonyms.entrySet()) {
            DrosophilaGene gene = new DrosophilaGene();
            gene.id = species.removeSpeciesFromGene(entry.getKey());
            gene.symbol = entry.getValue().stream()
                .filter(s -> s.type == Synonym.Type.SYMBOL)
                .map(s -> s.synonym)
                .findFirst()
                .orElse(null);
            gene.fullname = entry.getValue().stream()
                .filter(s -> s.type == Synonym.Type.NAME)
                .map(s -> s.synonym)
                .findFirst()
                .orElse(null);
            gene.annotationId = entry.getValue().stream()
                .filter(s -> s.type == Synonym.Type.OTHER && s.synonym.startsWith("CG"))
                .map(s -> s.synonym)
                .findFirst()
                .orElse(null);
            drosophilaGenes.add(gene);
        }
        return drosophilaGenes;
    }

    @GET
    @Path("/tribolium/genes")
    @Operation(summary = "Get information of a list of tribolium genes")
    @Parameters({
        @Parameter(name = "ids", description = "Comma-separated list of Tribolium gene identifiers in format TC[0-9]{6}}", example = "TC016177,TC001906")
    })
    public List<TriboliumGene> getTriboliumGenes(@DefaultValue("") @QueryParam("ids") String ids) {
        Set<String> idSet = trimAndSplit(ids, HashSet::new);
        Species species = mainSpecies;
        List<Synonym> synonyms = synonymIndex.findBySynonyms(idSet.stream().toList());

        List<String> genes = synonyms.stream()
            .filter(s -> s.type == Synonym.Type.CURRENT_ID || s.type == Synonym.Type.OLD_ID)
            .filter(s -> species.isGeneFromSpecies(s.gene))
            .map(s -> s.gene)
            .toList();
        
        var sequenceIds = synonymIndex.findByGenes(genes)
            .stream()
            .filter(s -> s.type == Synonym.Type.TRANSCRIPT || s.type == Synonym.Type.PROTEIN)
            .map(s -> {
                if (s.type == Synonym.Type.TRANSCRIPT) {
                    return List.of(
                        createSequenceSynonyms(s.synonym, species, SequenceType.TRANSCRIPT, s.gene),
                        createSequenceSynonyms(s.synonym, species, SequenceType.CDS, s.gene)
                    );
                } else {
                    return List.of(createSequenceSynonyms(s.synonym, species, SequenceType.PROTEIN, s.gene));
                }
            })
            .flatMap(s -> s.stream())
            .collect(Collectors.groupingBy(
                s -> s.gene,
                Collectors.mapping(s -> s.synonym, Collectors.toList())
            ));

        List<Sequence> sequences = sequenceIndex.findByIds(sequenceIds.values().stream().flatMap(List::stream).toList());

        return genomicLocationIndex.findByIds(genes).stream().map(g -> {
            TriboliumGene gene = new TriboliumGene();
            gene.id = species.removeSpeciesFromGene(g.gene);
            gene.start = g.start;
            gene.end = g.end;
            gene.strand = g.strand;
            gene.seqname = g.referenceSeq;
            gene.mRNAs = sequences.stream()
                .filter(s -> s.type == SequenceType.TRANSCRIPT)
                .filter(s -> sequenceIds.getOrDefault(g.gene, Collections.emptyList()).contains(s._id()))
                .map(s -> {
                    var seq = new TriboliumGene.Sequence();
                    seq.id = s.name;
                    seq.seq = s.sequence;
                    return seq;
                })
                .toList();
            gene.proteins = sequences.stream()
                .filter(s -> s.type == SequenceType.PROTEIN)
                .filter(s -> sequenceIds.getOrDefault(g.gene, Collections.emptyList()).contains(s._id()))
                .map(s -> {
                    var seq = new TriboliumGene.Sequence();
                    seq.id = s.name;
                    seq.seq = s.sequence;
                    return seq;
                })
                .toList();
            gene.CDS = sequences.stream()
                .filter(s -> s.type == SequenceType.CDS)
                .filter(s -> sequenceIds.getOrDefault(g.gene, Collections.emptyList()).contains(s._id()))
                .map(s -> {
                    var seq = new TriboliumGene.Sequence();
                    seq.id = s.name;
                    seq.seq = s.sequence;
                    return seq;
                })
                .toList();
            return gene;
        }).toList();
    }

    private Synonym createSequenceSynonyms(String synonym, Species species, SequenceType type, String gene) {
        Sequence seq = new Sequence();
        seq.name = synonym;
        seq.species = species.toString();
        seq.type = type;
        return new Synonym(gene, null, seq._id());
    }

    private static <C extends Collection<String>> C trimAndSplit(String str, Supplier<C> collectionFactory) {
        return Arrays.stream(str.trim().split(","))
                .map(String::trim)
                .filter(Predicate.not(""::equals))
                .collect(Collectors.toCollection(collectionFactory));
    }

    private List<SilencingSeq> listSilencingSeqsByIBnumbers(List<String> iBs) {
        Species species = Species.of("Tcas");
        Map<String, List<Synonym>> synonyms = synonymIndex.findBySynonyms(iBs)
            .stream()
            .filter(s -> s.type.equals(Synonym.Type.DSRNA))
            .collect(Collectors.groupingBy(s -> s.synonym));
        List<DsRNA> dsRNAs = dsRNAIndex.findByIds(iBs.stream().map(s -> species.createGeneId(s)).toList());
        List<SilencingSeq> silencingSeqs = new ArrayList<>();
        for (var dsRNA: dsRNAs) {
            var silencingSeq = new SilencingSeq();
            silencingSeq.id = species.removeSpeciesFromGene(dsRNA.id);
            silencingSeq.leftPrimer = dsRNA.leftPrimer;
            silencingSeq.rightPrimer = dsRNA.rightPrimer;
            silencingSeq.seq = dsRNA.seq;
            silencingSeq.geneIds = synonyms.getOrDefault(silencingSeq.id, Collections.emptyList())
                .stream()
                .map(s -> species.removeSpeciesFromGene(s.gene))
                .toList();
            silencingSeqs.add(silencingSeq);
        }
        return silencingSeqs;
    }

    private List<SilencingSeq> listSilencingSeqsByGeneIds(List<String> geneIds) {
        Species species = Species.of("Tcas");
        List<String> queries = geneIds.stream().map(id -> species.createGeneId(id)).toList();
        Map<String, List<Synonym>> synonyms = synonymIndex.findByGenes(queries)
            .stream()
            .filter(s -> s.type.equals(Synonym.Type.DSRNA))
            .collect(Collectors.groupingBy(s -> s.synonym));

        List<String> iBs = synonyms.values().stream().flatMap(List::stream).map(s -> s.synonym).toList();
        List<DsRNA> dsRNAs = dsRNAIndex.findByIds(iBs.stream().map(s -> species.createGeneId(s)).toList());
        List<SilencingSeq> silencingSeqs = new ArrayList<>();
        for (var dsRNA: dsRNAs) {
            var silencingSeq = new SilencingSeq();
            silencingSeq.id = species.removeSpeciesFromGene(dsRNA.id);
            silencingSeq.leftPrimer = dsRNA.leftPrimer;
            silencingSeq.rightPrimer = dsRNA.rightPrimer;
            silencingSeq.seq = dsRNA.seq;
            silencingSeq.geneIds = synonyms.getOrDefault(silencingSeq.id, Collections.emptyList())
                .stream()
                .map(s -> species.removeSpeciesFromGene(s.gene))
                .toList();
            silencingSeqs.add(silencingSeq);
        }
        return silencingSeqs;
    }

    private List<GroupedOrthology> getOrthology(Species species, String source, List<String> geneIds) {
        List<String> queries = geneIds.stream().map(id -> species.createGeneId(id)).toList();
        List<Orthology> orthologies = orthologyIndex.listByOrthologs(queries);
        Map<String, GroupedOrthology> groupedOrthologies = new HashMap<>();
        for (var orthology: orthologies) {
            String orthoSource = orthology.group.split(":")[0].split("\\.")[1];
            if (!Objects.equals(source.toLowerCase(), orthoSource.toLowerCase()) && !Objects.equals(source.toLowerCase(), "all")) {
                continue;
            }
            for (var dmelGene: queries) {
                if (!orthology.orthologs.contains(dmelGene)) {
                    continue;
                }
                List<GroupedOrthology.Ortholog> orthologs = new ArrayList<>();
                for (var orthologousGene: orthology.orthologs) {
                    Species otherSpecies = Species.ofGene(orthologousGene);
                    if (!otherSpecies.equals(species)) {
                        var ortholog = new GroupedOrthology.Ortholog();
                        ortholog.gene = otherSpecies.removeSpeciesFromGene(orthologousGene);
                        ortholog.source = orthoSource;
                        orthologs.add(ortholog);
                    }
                }
                groupedOrthologies.computeIfAbsent(dmelGene, k -> {
                    var r = new GroupedOrthology();
                    r.gene = species.removeSpeciesFromGene(dmelGene);
                    r.orthologs = new ArrayList<>();
                    return r;
                }).orthologs.addAll(orthologs);
            }
        }
        return groupedOrthologies.values().stream().toList();
    }
}
