package ibb.api.geneservice.startup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import ibb.api.geneservice.ortholog.OrthologIndex;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrthologLoader {
    
    @Inject
    OrthologIndex orthologIndex;

    public void load(String source, File dir) {
        File[] files = dir.listFiles(File::isFile);
        Optional.of(Arrays.stream(files).toList())
            .filter(orthologs -> !orthologs.isEmpty())
            .ifPresent(orthologs -> loadOrthologs(source, orthologs));
    }

    private void loadOrthologs(String source, List<File> files) {
        try {
            if (!orthologIndex.exists(source)) {
                orthologIndex.createIndex(source);
                for (var file : files) {
                    orthologIndex.loadOrthologs(source, file.toPath());
                };
            } else {
                Log.infov("Index for ortholog source {0} already exists", source);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
