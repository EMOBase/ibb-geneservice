package ibb.api.geneservice.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DataLoader {
    
    @ConfigProperty(name = "geneservice.data-dir")
    String dataDir;

    public Stream<File> listSpeciesDirs() {
        return Arrays.stream(Path.of(dataDir, "species").toFile().listFiles(File::isDirectory));
    }

    public File getOrthologyDir() {
        return Path.of(dataDir, "orthology").toFile();
    }
}
