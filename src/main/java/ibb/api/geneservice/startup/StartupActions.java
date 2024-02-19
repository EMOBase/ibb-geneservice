package ibb.api.geneservice.startup;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartupActions {

    @ConfigProperty(name = "geneservice.data-dir")
    String dataDir;

    @Inject
    SpeciesLoader speciesLoader;

    @Inject
    OrthologLoader orthologLoader;

    void init(@Observes StartupEvent event) {
        // Path allSpeciesDir = Path.of(dataDir, "species");
        // File[] speciesDirs = allSpeciesDir.toFile().listFiles(File::isDirectory);
        // for (File speciesDir : speciesDirs) {
        //     speciesLoader.load(speciesDir.getName(), speciesDir);
        // }

        Path allOrthologsDir = Path.of(dataDir, "orthologs");
        File[] orthologsDirs = allOrthologsDir.toFile().listFiles(File::isDirectory);
        for (File orthologsDir : orthologsDirs) {
            orthologLoader.load(orthologsDir.getName(), orthologsDir);
        }
    }
}
