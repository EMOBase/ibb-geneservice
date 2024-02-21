package ibb.api.geneservice.startup;

import java.io.File;
import java.util.Arrays;

import ibb.api.geneservice.domains.ortholog.OrthologParser;
import ibb.api.geneservice.index.DocumentSource;
import ibb.api.geneservice.index.IndexManager;
import ibb.api.geneservice.index.IndexType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrthologLoader {

    @Inject
    IndexManager indexManager;

    public void load(String orthoSource, File dir) {
        File[] files = dir.listFiles(File::isFile);
        var docSources = Arrays.stream(files)
            .map(file -> new DocumentSource<>(file, new OrthologParser(orthoSource)))
            .toList();

        indexManager.loadAllIfNotExists(indexManager.getPrefix(IndexType.ORTHOLOG), docSources);
    }
}
