package ibb.api.geneservice.utils;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class FileTypeHelper {
    
    public static final List<String> GZIP_EXTENSIONS = List.of(".gz", ".gzip");
    public static final List<String> FASTA_EXTENSIONS = List.of(".fa", ".fasta", ".fna", ".faa");
    public static final List<String> GFF_EXTENSIONS = List.of(".gff", ".gff3");

    /**
     * Check the suffix of a file name
     * @param name the file name
     * @return an empty {@code Optional} if the name does not end with that extension. Otherwise, an {@code Optional} of the name without the extension
     */
    public static Optional<String> endsWithSuffix(String name, List<String> extensions) {
        return extensions.stream()
            .filter(ext -> name.endsWith(ext))
            .findFirst()
            .map(ext -> name.replaceAll(ext + "$", ""));
    }

    public static String ignoreGzipSuffix(String name) {
        return ignoreSuffix(name, GZIP_EXTENSIONS);
    }

    public static String ignoreSuffix(String name, List<String> extensions) {
        return endsWithSuffix(name, extensions).orElse(name);
    }

    public static boolean isGFFFile(File file) {
        return endsWithSuffix(ignoreGzipSuffix(file.getName().toLowerCase()), GFF_EXTENSIONS).isPresent();
    }
}
