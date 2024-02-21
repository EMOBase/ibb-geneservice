package ibb.api.geneservice.startup;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

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
        subDirs("species").forEach(speciesDir -> speciesLoader.load(speciesDir.getName(), speciesDir));
        subDirs("orthologs").forEach(orthologDir -> orthologLoader.load(orthologDir.getName(), orthologDir));
    }

    private Stream<File> subDirs(String dir) {
        return Arrays.stream(Path.of(dataDir, dir).toFile().listFiles(File::isDirectory));
    }
}
