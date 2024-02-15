package ibb.api.geneservice.startup;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ibb.api.geneservice.index.GeneIndex;
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
            String prefix = file.getName().replaceAll(".gz$", "");
            if (prefix.endsWith(".gff") || prefix.endsWith(".gff3")) {
                fileMap.computeIfAbsent("gff", k -> new ArrayList<>()).add(file);
            }
        }

        Optional.ofNullable(fileMap.get("gff")).ifPresent(gffs -> loadGFFs(species, gffs));
    }

    private void loadGFFs(String species, List<File> files) {
        if (!geneIndex.exists(species)) {
            geneIndex.createIndex(species);
            files.forEach(file -> {
                geneIndex.loadFromGFF(species, file.toPath());
            });
        } else {
            Log.infov("Gene index for species {0} already exists", species);
        }
    }
}
