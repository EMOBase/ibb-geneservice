package ibb.api.geneservice.startup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ibb.api.geneservice.genomic.GenomicIndex;
import ibb.api.geneservice.sequence.SequenceIndex;
import ibb.api.geneservice.sequence.SequenceType;
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
    GenomicIndex geneIndex;

    @Inject
    SequenceIndex sequenceIndex;

    void init(@Observes StartupEvent event) {
        Path dir = Path.of(dataDir, "species");
        File[] speciesDirs = dir.toFile().listFiles(File::isDirectory);
        for (File speciesDir : speciesDirs) {
            loadSpecies(speciesDir.getName(), speciesDir);
        }
    }

    private void loadSpecies(String species, File dir) {
        File[] files = dir.listFiles(File::isFile);
        Map<String, List<File>> fileMap = new HashMap<>();

        for (File file : files) {
            if (FileTypes.isGFFFile(file)) {
                fileMap.computeIfAbsent("gff", k -> new ArrayList<>()).add(file);
            } else if (FileTypes.isCDSFile(file)) {
                fileMap.computeIfAbsent("cds", k -> new ArrayList<>()).add(file);
            } else if (FileTypes.isRNAFile(file)) {
                fileMap.computeIfAbsent("rna", k -> new ArrayList<>()).add(file);
            } else if (FileTypes.isProteinFile(file)) {
                fileMap.computeIfAbsent("protein", k -> new ArrayList<>()).add(file);
            }
        }

        Optional.ofNullable(fileMap.get("gff"))
            .ifPresent(gffs -> loadGFFs(species, gffs));
        Optional.ofNullable(fileMap.get("cds"))
            .ifPresent(cds -> loadSequences(species, SequenceType.CDS, cds));
        Optional.ofNullable(fileMap.get("rna"))
            .ifPresent(rnas -> loadSequences(species, SequenceType.RNA, rnas));
        Optional.ofNullable(fileMap.get("protein"))
            .ifPresent(proteins -> loadSequences(species, SequenceType.PROTEIN, proteins));
    }

    private void loadGFFs(String species, List<File> files) {
        try {
            if (!geneIndex.exists(species)) {
                geneIndex.createIndex(species);
                for (var file : files) {
                    geneIndex.loadFromGFF(species, file.toPath());
                };
            } else {
                Log.infov("Gene index for species {0} already exists", species);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadSequences(String species, SequenceType type, List<File> files) {
        try {
            if (!sequenceIndex.exists(species, type)) {
                sequenceIndex.createIndex(species, type);
                for (var file : files) {
                    sequenceIndex.loadFromFasta(species, type, file.toPath());
                };
            } else {
                Log.infov("{0} index for species {1} already exists", type, species);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
