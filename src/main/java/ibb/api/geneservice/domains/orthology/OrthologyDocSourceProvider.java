package ibb.api.geneservice.domains.orthology;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import ibb.api.geneservice.es.ESDocSource;
import ibb.api.geneservice.es.ESDocSourceProvider;
import ibb.api.geneservice.utils.DataLoader;
import ibb.api.geneservice.utils.FileTypeHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrthologyDocSourceProvider implements ESDocSourceProvider<Orthology> {

    @Inject
    DataLoader dataLoader;

    @Override
    public Stream<ESDocSource<Orthology>> provideDocSources() {
        File[] files = dataLoader.getOrthologyDir().listFiles(File::isFile);
        return Arrays.stream(files)
            .map(file -> {
                String orthoPrefix = FileTypeHelper.ignoreSuffix(file.getName(), List.of(".tsv", ".txt"));
                String orthoSource = FileTypeHelper.ignoreSuffix(orthoPrefix, List.of("_orthology"));
                return new ESDocSource<>(file, new OrthologyParser(orthoSource));
            });
    }
}
