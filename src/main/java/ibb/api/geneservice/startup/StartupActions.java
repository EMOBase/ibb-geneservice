package ibb.api.geneservice.startup;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ibb.api.geneservice.domains.genomic.GenomicLocation;
import ibb.api.geneservice.domains.genomic.GenomicLocationIndex;
import ibb.api.geneservice.domains.genomic.GenomicLocationParser;
import ibb.api.geneservice.domains.ortholog.OrthologIndex;
import ibb.api.geneservice.domains.ortholog.OrthologParser;
import ibb.api.geneservice.domains.sequence.Sequence;
import ibb.api.geneservice.domains.sequence.SequenceIndex;
import ibb.api.geneservice.domains.sequence.SequenceParser;
import ibb.api.geneservice.domains.sequence.SequenceType;
import ibb.api.geneservice.domains.synonym.FlyBaseGeneRNAProteinMapParser;
import ibb.api.geneservice.domains.synonym.FlyBaseSynonymParser;
import ibb.api.geneservice.domains.synonym.GFF3SynonymParser;
import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.es.DocumentSource;
import ibb.api.geneservice.parser.TextParser;
import ibb.api.geneservice.parser.gff3.GFF3GeneIDFinder;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartupActions {

    @ConfigProperty(name = "geneservice.data-dir")
    String dataDir;

    @Inject
    GenomicLocationIndex genomicLocationIndex;

    @Inject
    SequenceIndex sequenceIndex;

    @Inject
    SynonymIndex synonymIndex;

    @Inject
    OrthologIndex orthologIndex;

    public void init(@Observes StartupEvent event) {

        if (!genomicLocationIndex.exists()) {
            listSubDirs("species")
                .map(this::getGenomicLocationSources)
                .flatMap(s -> s)
                .forEach(genomicLocationIndex::load);
        } else {
            Log.info("Genomic location index already exists");
        }

        if (!sequenceIndex.exists()) {
            listSubDirs("species")
                .map(this::getSequenceSources)
                .flatMap(s -> s)
                .forEach(sequenceIndex::load);
        } else {
            Log.info("Sequence index already exists");
        }

        boolean shouldReloadOrthologs = false;
        if (!synonymIndex.exists()) {
            listSubDirs("species")
                .map(this::getSynonymSources)
                .flatMap(s -> s)
                .forEach(synonymIndex::load);

            synonymIndex.refresh();
            if (synonymIndex.exists()) {
                Log.info("Computing enriched index from synonym index");
                orthologIndex.computeEnrichedIndex();
                shouldReloadOrthologs = true;
            }
        } else {
            Log.info("Synonym index already exists");
        }

        String orthologPipeline = orthologIndex.createPipeline();
        if (!orthologIndex.exists() || shouldReloadOrthologs) {
            orthologIndex.delete();
            listSubDirs("orthologs")
                .map(orthologDir -> {
                    File[] files = orthologDir.listFiles(File::isFile);
                    String orthoSource = orthologDir.getName();
                    return Arrays.stream(files)
                        .map(file -> new DocumentSource<>(file, new OrthologParser(orthoSource))
                            .withIngestPipeline(orthologPipeline));
                })
                .flatMap(s -> s)
                .forEach(orthologIndex::load);
        } else {
            Log.info("Ortholog index already exists");
        }

        Log.info("All data loaded");
    }

    private Stream<DocumentSource<GenomicLocation>> getGenomicLocationSources(File dir) {
        String species = dir.getName();
        return Arrays.stream(dir.listFiles(File::isFile))
            .filter(FileTypes::isGFFFile)
            .map(file -> {
                GenomicLocationParser parser;
                if (Objects.equals(species, "tribolium_castaneum")) {
                    parser = new GenomicLocationParser(GFF3GeneIDFinder.byTCLocusTag());
                } else {
                    parser = new GenomicLocationParser(GFF3GeneIDFinder.byNCBIGeneID());
                }
                return new DocumentSource<>(file, parser).withTags(List.of(species));
            });
    }

    private Stream<DocumentSource<Sequence>> getSequenceSources(File dir) {
        String species = dir.getName();
        return Arrays.stream(dir.listFiles(File::isFile))
            .map(file -> {
                SequenceParser parser = null;
                if (FileTypes.isCDSFile(file)) {
                    parser = new SequenceParser(SequenceType.CDS);
                } else if (FileTypes.isTranscriptFile(file)) {
                    parser = new SequenceParser(SequenceType.TRANSCRIPT);
                } else if (FileTypes.isProteinFile(file)) {
                    parser = new SequenceParser(SequenceType.PROTEIN);
                }
                if (parser == null) {
                    return null;
                } else {
                    return new DocumentSource<>(file, parser).withTags(List.of(species));
                }
            })
            .filter(Objects::nonNull);
    }

    private Stream<DocumentSource<Synonym>> getSynonymSources(File dir) {
        String species = dir.getName();
        return Arrays.stream(dir.listFiles(File::isFile))
            .map(file -> {
                TextParser<Synonym> parser = null;
                if (Objects.equals("drosophila_melanogaster", species)) {
                    if (FileTypes.isFlyBaseSynonymFile(file)) {
                        parser = new FlyBaseSynonymParser();
                    } else if (FileTypes.isFlyBaseGeneRNAProteinMapFile(file)) {
                        parser = new FlyBaseGeneRNAProteinMapParser();
                    }
                }
                if (FileTypes.isGFFFile(file)) {
                    if (Objects.equals(species, "tribolium_castaneum")) {
                        parser = new GFF3SynonymParser(GFF3GeneIDFinder.byTCLocusTag());
                    } else {
                        parser = new GFF3SynonymParser(GFF3GeneIDFinder.byNCBIGeneID());
                    }
                }
                if (parser != null) {
                    return new DocumentSource<>(file, parser).withTags(List.of(species));
                }
                return null;
            })
            .filter(Objects::nonNull);
    }

    // void init(@Observes StartupEvent event) throws IOException {
    //     if (forceReloadSpecies) {
    //         sourceIndexManager.delete(SourceIndexType.GENOMIC_LOCATION);
    //         sourceIndexManager.delete(SourceIndexType.SEQUENCE);
    //         sourceIndexManager.delete(SourceIndexType.SYNONYM);
    //         pipelineManager.delete(PipelineType.ADD_GENE_TO_ORTHOLOG);
    //     }
    //     subDirs("species").forEach(speciesDir -> speciesLoader.load(speciesDir.getName(), speciesDir));


    //     if (forceReloadOrthologs) {
    //         sourceIndexManager.delete(SourceIndexType.ORTHOLOG);
    //     }
    //     subDirs("orthologs").forEach(orthologDir -> {
    //         File[] files = orthologDir.listFiles(File::isFile);
    //         String orthoSource = orthologDir.getName();
    //         var docSources = Arrays.stream(files)
    //             .map(file -> new DocumentSource<>(file, new OrthologParser(orthoSource), pipeline))
    //             .toList();
    //         sourceIndexManager.loadAllIfNotExists(docSources, SourceIndexType.ORTHOLOG);
    //     });
    // }

    private Stream<File> listSubDirs(String dir) {
        return Arrays.stream(Path.of(dataDir, dir).toFile().listFiles(File::isDirectory));
    }

    // private Optional<String> createOrthologIngestPipeline() throws IOException {
    //     if (!sourceIndexManager.exists(SourceIndexType.SYNONYM)) {
    //         return Optional.empty();
    //     }

    //     String policyName = esHelper.getESName("synonym2gene");
    //     String pipelineName = esHelper.getESName("add_gene_to_ortholog");

    //     if (forceRecreateEnrichedIndex) {
    //         try {
    //             esClient.ingest().deletePipeline(p -> p.id(pipelineName));
    //         } catch (ElasticsearchException e) {
    //             if (e.status() != 404) {
    //                 throw e;
    //             }
    //         }
    //         try {
    //             esClient.enrich().deletePolicy(p -> p.name(policyName));
    //         } catch (ElasticsearchException e) {
    //             if (e.status() != 404) {
    //                 throw e;
    //             }
    //         }
    //     }

    //     boolean policyExists = esClient.enrich().getPolicy(p -> p.name(policyName)).policies().size() > 0;
    //     if (!policyExists) {
    //         esClient.enrich().putPolicy(p -> p
    //             .name(policyName)
    //             .match(m -> m
    //                 .indices(sourceIndexManager.getIndexName(SourceIndexType.SYNONYM))
    //                 .matchField("value")
    //                 .enrichFields("gene")
    //             ));
    //         esClient.indices().refresh(r -> r
    //             .index(sourceIndexManager.getIndexName(SourceIndexType.SYNONYM))
    //         );
    //         esClient.enrich().executePolicy(p -> p
    //             .name(policyName)
    //             .waitForCompletion(true)
    //         );
    //     }

    //     esClient.ingest().putPipeline(p -> p
    //         .id(pipelineName)
    //         .description("Enriches ortholog documents with current gene name")
    //         .processors(pr -> pr
    //             .enrich(e -> e
    //                 .policyName(policyName)
    //                 .field("ortholog")
    //                 .targetField("gene_enriched")
    //             ))
    //         .processors(pr -> pr
    //             .set(s -> s
    //                 .field("gene")
    //                 .value(JsonData.of("{{{gene_enriched.gene}}}"))
    //             ))
    //         .processors(pr -> pr
    //             .set(s -> s
    //                 .field("gene")
    //                 .value(JsonData.of("{{{ortholog}}}"))
    //                 .if_("ctx.gene == ''")
    //         ))
    //         .processors(pr -> pr
    //             .remove(r -> r
    //                 .field("gene_enriched")
    //                 .ignoreFailure(true)
    //             ))
    //     );
    //     return Optional.of(pipelineName);
    // }
}
