package ibb.api.geneservice.startup;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ibb.api.geneservice.domains.genomic.GenomicLocation;
import ibb.api.geneservice.domains.genomic.GenomicLocationIndex;
import ibb.api.geneservice.domains.genomic.GenomicLocationParser;
import ibb.api.geneservice.domains.orthology.OrthologyIndex;
import ibb.api.geneservice.domains.orthology.OrthologyParser;
import ibb.api.geneservice.domains.sequence.Sequence;
import ibb.api.geneservice.domains.sequence.SequenceIndex;
import ibb.api.geneservice.domains.sequence.SequenceParser;
import ibb.api.geneservice.domains.sequence.SequenceType;
import ibb.api.geneservice.domains.synonym.FlyBaseGeneRNAProteinMapParser;
import ibb.api.geneservice.domains.synonym.FlyBaseSynonymParser;
import ibb.api.geneservice.domains.synonym.GFF3SynonymParser;
import ibb.api.geneservice.domains.synonym.Synonym;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.es.ESDocSource;
import ibb.api.geneservice.parser.TextParser;
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

    @ConfigProperty(name = "geneservice.elasticsearch.reload-orthology", defaultValue = "false")
    boolean reloadOrthology;

    @Inject
    GenomicLocationIndex genomicLocationIndex;

    @Inject
    SequenceIndex sequenceIndex;

    @Inject
    SynonymIndex synonymIndex;

    @Inject
    OrthologyIndex orthologyIndex;

    public void init(@Observes StartupEvent event) {
        reloadGenomicLocations = reloadGenomicLocations || !genomicLocationIndex.exists();
        if (reloadGenomicLocations) {
            genomicLocationIndex.delete();
            genomicLocationIndex.setup();
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
            sequenceIndex.setup();
            listSubDirs("species")
                .map(this::getSequenceSources)
                .flatMap(s -> s)
                .forEach(sequenceIndex::load);
        } else {
            Log.info("Sequence index already exists");
        }

        reloadOrthology = reloadOrthology || !orthologyIndex.exists();
        reloadSynonyms = reloadSynonyms || !synonymIndex.exists();

        if (reloadSynonyms) {
            synonymIndex.delete();
            synonymIndex.setup();

            listSubDirs("species")
                .map(this::getSynonymSources)
                .flatMap(s -> s)
                .forEach(synonymIndex::load);

            synonymIndex.refresh();
            if (synonymIndex.exists()) {
                Log.info("Computing enriched indices from synonym index");
                synonymIndex.computeGene2SynonymsEnrichedIndex();
                reloadOrthology = true;
            }
        } else {
            Log.info("Synonym index already exists");
        }

        if (reloadOrthology) {
            orthologyIndex.delete();
            orthologyIndex.setup();
            File[] files = Path.of(dataDir, "orthology").toFile().listFiles(File::isFile);
            Arrays.stream(files)
                .map(file -> {
                    String orthoSource = file.getName().split("_")[0];
                    return new ESDocSource<>(file, new OrthologyParser(orthoSource));
                })
                .forEach(orthologyIndex::load);
        } else {
            Log.info("Ortholog index already exists");
        }

        Log.info("All data loaded");
    }

    private Stream<ESDocSource<GenomicLocation>> getGenomicLocationSources(File dir) {
        String species = dir.getName();
        return Arrays.stream(dir.listFiles(File::isFile))
            .filter(FileTypes::isGFFFile)
            .map(file -> new ESDocSource<>(file, new GenomicLocationParser(species)));
    }

    private Stream<ESDocSource<Sequence>> getSequenceSources(File dir) {
        String species = dir.getName();
        return Arrays.stream(dir.listFiles(File::isFile))
            .map(file -> {
                SequenceParser parser = null;
                if (FileTypes.isCDSFile(file)) {
                    parser = new SequenceParser(species, SequenceType.CDS);
                } else if (FileTypes.isTranscriptFile(file)) {
                    parser = new SequenceParser(species, SequenceType.TRANSCRIPT);
                } else if (FileTypes.isProteinFile(file)) {
                    parser = new SequenceParser(species, SequenceType.PROTEIN);
                }
                if (parser == null) {
                    return null;
                } else {
                    return new ESDocSource<>(file, parser);
                }
            })
            .filter(Objects::nonNull);
    }

    private Stream<ESDocSource<Synonym>> getSynonymSources(File dir) {
        String species = dir.getName();
        return Arrays.stream(dir.listFiles(File::isFile))
            .map(file -> {
                TextParser<Synonym> parser = null;
                if (Objects.equals("Dmel", species)) {
                    if (FileTypes.isFlyBaseSynonymFile(file)) {
                        parser = new FlyBaseSynonymParser(species);
                    } else if (FileTypes.isFlyBaseGeneRNAProteinMapFile(file)) {
                        parser = new FlyBaseGeneRNAProteinMapParser(species);
                    }
                }
                if (FileTypes.isGFFFile(file)) {
                    parser = new GFF3SynonymParser(species);
                }
                if (parser != null) {
                    return new ESDocSource<>(file, parser);
                }
                return null;
            })
            .filter(Objects::nonNull);
    }

    private Stream<File> listSubDirs(String dir) {
        return Arrays.stream(Path.of(dataDir, dir).toFile().listFiles(File::isDirectory));
    }
}
