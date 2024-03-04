package ibb.api.geneservice.domains.sequence;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import ibb.api.geneservice.es.ESDocSource;
import ibb.api.geneservice.es.ESDocSourceProvider;
import ibb.api.geneservice.utils.DataLoader;
import ibb.api.geneservice.utils.FileTypeHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SequenceDocSourceProvider implements ESDocSourceProvider<Sequence> {

    private static final List<String> CDS_SUFFICES = List.of("_cds");
    private static final List<String> TRANSCRIPT_SUFFICES = List.of("_rna", "_mrna", "_rnas", "_mrnas");
    private static final List<String> PROTEIN_SUFFICES = List.of("_protein", "_proteins");

    @Inject
    DataLoader dataLoader;

    @Override
    public Stream<ESDocSource<Sequence>> provideDocSources() {
        return dataLoader.listSpeciesDirs()
            .map(this::findSequenceSources)
            .flatMap(s -> s);
    }

    private Stream<ESDocSource<Sequence>> findSequenceSources(File speciesDir) {
        String species = speciesDir.getName();
        return Arrays.stream(speciesDir.listFiles(File::isFile))
            .map(file -> {
                SequenceParser parser = null;
                if (isCDSFile(file)) {
                    parser = new SequenceParser(species, SequenceType.CDS);
                } else if (isTranscriptFile(file)) {
                    parser = new SequenceParser(species, SequenceType.TRANSCRIPT);
                } else if (isProteinFile(file)) {
                    parser = new SequenceParser(species, SequenceType.PROTEIN);
                }
                if (parser == null) {
                    return null;
                } else {
                    return new ESDocSource<>(file, parser);
                }
            })
            .filter(Objects::nonNull);
    }

    private static boolean isCDSFile(File file) {
        return filenameHasSuffix(file, CDS_SUFFICES);
    }

    private static boolean isTranscriptFile(File file) {
        return filenameHasSuffix(file, TRANSCRIPT_SUFFICES);
    }

    private static boolean isProteinFile(File file) {
        return filenameHasSuffix(file, PROTEIN_SUFFICES);
    }

    private static boolean filenameHasSuffix(File file, List<String> suffices) {
        String name = FileTypeHelper.ignoreGzipSuffix(file.getName().toLowerCase());

        Optional<String> nameWithoutFastaOptional = FileTypeHelper.endsWithSuffix(name, FileTypeHelper.FASTA_EXTENSIONS);
        if (nameWithoutFastaOptional.isEmpty()) {
            return false;
        }
        String nameWithoutFasta = nameWithoutFastaOptional.get();
        return FileTypeHelper.endsWithSuffix(nameWithoutFasta, suffices)
            .map(n -> true)
            .orElse(false);
    }
}
