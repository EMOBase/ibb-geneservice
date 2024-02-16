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

import ibb.api.geneservice.index.GeneIndex;
import ibb.api.geneservice.index.SequenceIndex;
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
    GeneIndex geneIndex;

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

        Optional.ofNullable(fileMap.get("gff")).ifPresent(gffs -> loadGFFs(species, gffs));
        List.of("cds", "rna", "protein").forEach(type -> {
            Optional.ofNullable(fileMap.get(type))
                .ifPresent(filesOfType -> loadSequences(species, type, filesOfType));
        });
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

    private void loadSequences(String species, String type, List<File> files) {
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
