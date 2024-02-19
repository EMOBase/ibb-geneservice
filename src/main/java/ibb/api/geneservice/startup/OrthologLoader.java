package ibb.api.geneservice.startup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import ibb.api.geneservice.domains.ortholog.OrthologIndex;
import ibb.api.geneservice.domains.ortholog.OrthologParser;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrthologLoader {

    public void load(String source, File dir) {
        File[] files = dir.listFiles(File::isFile);
        Optional.of(Arrays.stream(files).toList())
            .filter(orthologs -> !orthologs.isEmpty())
            .ifPresent(orthologs -> loadOrthologs(source, orthologs));
    }

    private void loadOrthologs(String source, List<File> files) {
        try {
            OrthologIndex index = new OrthologIndex(source);
            if (!index.exists()) {
                for (var file : files) {
                    index.load(file.toPath(), new OrthologParser());
                };
            } else {
                Log.infov("Orthologs for {0} already exists", source);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
