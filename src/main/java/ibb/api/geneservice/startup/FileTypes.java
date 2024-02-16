package ibb.api.geneservice.startup;

import java.io.File;
import java.util.List;

/**
 * Determine the type of file based on its name
 */
public class FileTypes {
    
    private static final List<String> FASTA_EXTENSIONS = List.of(".fa", ".fasta", ".fna", ".faa");
    private static final List<String> GFF_EXTENSIONS = List.of(".gff", ".gff3");
    private static final List<String> GZIP_EXTENSIONS = List.of(".gz");
    private static final List<String> CDS_SUFFICES = List.of("_cds");
    private static final List<String> RNA_SUFFICES = List.of("_rna", "_mrna", "_rnas", "_mrnas");
    private static final List<String> PROTEIN_SUFFICES = List.of("_protein", "_proteins");


    public static boolean isGFFFile(File file) {
        String name = ignoreGzip(file.getName().toLowerCase());
        return GFF_EXTENSIONS.stream().anyMatch(ext -> name.endsWith(ext));
    }

    public static boolean isCDSFile(File file) {
        String name = ignoreGzip(file.getName().toLowerCase());
        return FASTA_EXTENSIONS.stream()
            .filter(ext -> name.endsWith(ext))
            .findFirst()
            .map(ext -> name.replaceAll(ext + "$", ""))
            .map(nameWithoutExt -> CDS_SUFFICES.stream().anyMatch(nameWithoutExt::endsWith))
            .orElse(false);
    }

    public static boolean isRNAFile(File file) {
        String name = ignoreGzip(file.getName().toLowerCase());
        return FASTA_EXTENSIONS.stream()
            .filter(ext -> name.endsWith(ext))
            .findFirst()
            .map(ext -> name.replaceAll(ext + "$", ""))
            .map(nameWithoutExt -> RNA_SUFFICES.stream().anyMatch(nameWithoutExt::endsWith))
            .orElse(false);
    }

    public static boolean isProteinFile(File file) {
        String name = ignoreGzip(file.getName().toLowerCase());
        return FASTA_EXTENSIONS.stream()
            .filter(ext -> name.endsWith(ext))
            .findFirst()
            .map(ext -> name.replaceAll(ext + "$", ""))
            .map(nameWithoutExt -> PROTEIN_SUFFICES.stream().anyMatch(nameWithoutExt::endsWith))
            .orElse(false);
    }

    private static String ignoreGzip(String name) {
        return GZIP_EXTENSIONS.stream()
            .filter(ext -> name.endsWith(ext))
            .findFirst()
            .map(ext -> name.replaceAll(ext + "$", ""))
            .orElse(name);
    }
}
