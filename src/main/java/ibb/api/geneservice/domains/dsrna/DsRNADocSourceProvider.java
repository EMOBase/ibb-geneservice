package ibb.api.geneservice.domains.dsrna;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import ibb.api.geneservice.es.ESDocSource;
import ibb.api.geneservice.es.ESDocSourceProvider;
import ibb.api.geneservice.utils.DataLoader;
import ibb.api.geneservice.utils.Species;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DsRNADocSourceProvider implements ESDocSourceProvider<DsRNA> {

    @Inject
    DataLoader dataLoader;

    @Override
    public Stream<ESDocSource<DsRNA>> provideDocSources() {
        return dataLoader.listSpeciesDirs()
            .map(this::findDocSources)
            .flatMap(s -> s);
    }
 
    private Stream<ESDocSource<DsRNA>> findDocSources(File speciesDir) {
        Species species = Species.of(speciesDir.getName());
        File[] files = speciesDir.listFiles(File::isFile);
        if (files == null) {
            return Stream.empty();
        }
        return Arrays.stream(files)
            .map(file -> {
                if (Objects.equals(species, Species.of("Tcas")) && isIBSequenceFile(file)) {
                    return new ESDocSource<>(file, new DsRNAParser(species));
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull);
    }

    private boolean isIBSequenceFile(File file) {
        return file.getName().toLowerCase().startsWith("ib_sequence");
    }
}
