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
import ibb.api.geneservice.domains.ortholog.OrthogroupIndex;
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

    @ConfigProperty(name = "geneservice.elasticsearch.reload-genomiclocations", defaultValue = "false")
    boolean reloadGenomicLocations;

    @ConfigProperty(name = "geneservice.elasticsearch.reload-sequences", defaultValue = "false")
    boolean reloadSequences;

    @ConfigProperty(name = "geneservice.elasticsearch.reload-synonyms", defaultValue = "false")
    boolean reloadSynonyms;

    @ConfigProperty(name = "geneservice.elasticsearch.reload-orthologs", defaultValue = "false")
    boolean reloadOrthologs;

    @Inject
    GenomicLocationIndex genomicLocationIndex;

    @Inject
    SequenceIndex sequenceIndex;

    @Inject
    SynonymIndex synonymIndex;

    @Inject
    OrthologIndex orthologIndex;

    @Inject
    OrthogroupIndex orthogroupIndex;

    public void init(@Observes StartupEvent event) {
        reloadGenomicLocations = reloadGenomicLocations || !genomicLocationIndex.exists();
        if (reloadGenomicLocations) {
            genomicLocationIndex.delete();
            listSubDirs("species")
                .map(this::getGenomicLocationSources)
                .flatMap(s -> s)
                .forEach(genomicLocationIndex::load);
        } else {
            Log.info("Genomic location index already exists.");
        }

        reloadSequences = reloadSequences || !sequenceIndex.exists();
        if (reloadSequences) {
            sequenceIndex.delete();
            listSubDirs("species")
                .map(this::getSequenceSources)
                .flatMap(s -> s)
                .forEach(sequenceIndex::load);
        } else {
            Log.info("Sequence index already exists");
        }

        reloadOrthologs = reloadOrthologs || !orthologIndex.exists();
        reloadSynonyms = reloadSynonyms || !synonymIndex.exists();

        if (reloadSynonyms) {
            orthologIndex.delete();
            orthogroupIndex.delete();
            synonymIndex.delete();

            listSubDirs("species")
                .map(this::getSynonymSources)
                .flatMap(s -> s)
                .forEach(synonymIndex::load);

            synonymIndex.refresh();
            if (synonymIndex.exists()) {
                Log.info("Computing enriched indices from synonym index");
                synonymIndex.computeSynonym2GeneEnrichedIndex();
                synonymIndex.computeGene2SynonymsEnrichedIndex();
                reloadOrthologs = true;
            }
        } else {
            Log.info("Synonym index already exists");
        }

        if (reloadOrthologs) {
            orthologIndex.delete();
            String orthologPipeline = orthologIndex.createPipeline();
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
            if (orthologIndex.exists()) {
                orthologIndex.refresh();
                Log.info("Grouping orthologs by group");
                orthogroupIndex.delete();
                orthogroupIndex.transform();
            }
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

    private Stream<File> listSubDirs(String dir) {
        return Arrays.stream(Path.of(dataDir, dir).toFile().listFiles(File::isDirectory));
    }
}
