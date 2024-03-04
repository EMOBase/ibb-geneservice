package ibb.api.geneservice.domains.genomic;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import ibb.api.geneservice.es.ESDocSource;
import ibb.api.geneservice.es.ESDocSourceProvider;
import ibb.api.geneservice.utils.DataLoader;
import ibb.api.geneservice.utils.FileTypeHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GenomicLocationDocSourceProvider implements ESDocSourceProvider<GenomicLocation> {

    @Inject
    DataLoader dataLoader;

    @Override
    public Stream<ESDocSource<GenomicLocation>> provideDocSources() {
        return dataLoader.listSpeciesDirs()
            .map(this::findGenomicLocationSources)
            .flatMap(s -> s);
    }

    private Stream<ESDocSource<GenomicLocation>> findGenomicLocationSources(File speciesDir) {
        String species = speciesDir.getName();
        return Arrays.stream(speciesDir.listFiles(File::isFile))
            .filter(FileTypeHelper::isGFFFile)
            .map(file -> new ESDocSource<>(file, new GenomicLocationParser(species)));
    }
}
