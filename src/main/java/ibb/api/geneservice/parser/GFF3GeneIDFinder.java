package ibb.api.geneservice.parser;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface GFF3GeneIDFinder {
    Optional<String> findGeneId(GFF3Record record);

    public static GFF3GeneIDFinder byNCBIGeneID() {
        return record -> {
            for (String value: record.getAttribute("Dbxref")) {
                    String[] pair = value.split(":");
                    String db = pair[0];
                    String xrefId = pair[1];
                    if (Objects.equals("GeneID", db)) {
                        return Optional.of(xrefId);
                    }
                }
                return Optional.empty();
        };
    }

    public static GFF3GeneIDFinder byTCLocusTag() {
        return record -> {
            String locusTag = record.getAttributeFirstValue("locus_tag");
            if (locusTag != null) {
                Pattern pattern = Pattern.compile("TC[0-9]{6}");
                Matcher matcher = pattern.matcher(locusTag);
                if (matcher.find()) {
                    return Optional.of(matcher.group());
                }
            }
            return Optional.empty();
        };
    }
}
