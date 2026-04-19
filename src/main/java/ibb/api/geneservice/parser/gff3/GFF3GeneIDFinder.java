package ibb.api.geneservice.parser.gff3;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface GFF3GeneIDFinder {
    Optional<GFF3GeneID> findGeneId(GFF3Record record);

    public static GFF3GeneIDFinder byNCBIGeneID() {
        return record -> {
            for (String value: record.getAttribute("Dbxref")) {
                String[] pair = value.split(":");
                String db = pair[0];
                String xrefId = pair[1];
                if (Objects.equals("GeneID", db)) {
                    return Optional.of(new GFF3GeneID(xrefId, List.of()));
                }
            }
            return Optional.empty();
        };
    }

    public static GFF3GeneIDFinder byLocusTag() {
        return record -> {
            Optional<String> current = record.getAttributeFirstValueOptional("locus_tag");
            if (current.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new GFF3GeneID(current.get(), List.of()));
        };
    }

    public static GFF3GeneIDFinder byTCLocusTag() {
        return record -> {
            Pattern pattern = Pattern.compile("TC[0-9]{6}");
            Optional<String> current = record.getAttributeFirstValueOptional("locus_tag")
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(Matcher::group);
                
            if (current.isEmpty()) {
                return Optional.empty();
            }
            List<String> previous = record.getAttributeFirstValueOptional("old_locus_tag")
                .map(v -> v.split(","))
                .map(v -> Arrays.stream(v).map(pattern::matcher).filter(Matcher::find).map(Matcher::group).toList())
                .orElse(List.of());

            return Optional.of(new GFF3GeneID(current.get(), previous));
        };
    }
}
