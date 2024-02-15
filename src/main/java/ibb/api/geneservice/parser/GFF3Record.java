package ibb.api.geneservice.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GFF3Record {

    public static enum Strand {
        FORWARD("+"),
        REVERSE("-"),
        UNKNOWN("?");

        private static final Map<String, Strand> lookup = Arrays.stream(Strand.values())
            .collect(Collectors.toMap(Strand::getSymbol, Function.identity()));

        private final String symbol;
        private Strand(String symbol) {
            this.symbol = symbol;
        }
        public String getSymbol() {
            return symbol;
        }
        public static Strand getBySymbol(String symbol) {
            return Optional.ofNullable(lookup.get(symbol))
                .orElseThrow(() -> new IllegalArgumentException("Invalid strand: " + symbol));
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    private final String seqId;
    private final String source;
    private final String type;
    private final Integer start;
    private final Integer end;
    private final Float score;
    private final Strand strand;
    private final Integer phase;
    private final Map<String, List<String>> attributes;

    public static class Builder {
        private String seqId;
        private String source;
        private String type;
        private Integer start;
        private Integer end;
        private Float score;
        private Strand strand;
        private Integer phase;
        private Map<String, List<String>> attributes = new HashMap<>();

        public GFF3Record build() {
            return new GFF3Record(this);
        }
        public Builder setSeqId(String seqId) {
            this.seqId = seqId;
            return this;
        }
        public Builder setSource(String source) {
            this.source = source;
            return this;
        }
        public Builder setType(String type) {
            this.type = type;
            return this;
        }
        public Builder setStart(Integer start) {
            this.start = start;
            return this;
        }
        public Builder setEnd(Integer end) {
            this.end = end;
            return this;
        }
        public Builder setScore(Float score) {
            this.score = score;
            return this;
        }
        public Builder setStrand(Strand strand) {
            this.strand = strand;
            return this;
        }
        public Builder setPhase(Integer phase) {
            this.phase = phase;
            return this;
        }
        public Builder addAttribute(String key, List<String> values) {
            this.attributes.put(key, Collections.unmodifiableList(values));
            return this;
        }
    }

    private GFF3Record(Builder builder) {
        seqId = builder.seqId;
        source = builder.source;
        type = builder.type;
        start = builder.start;
        end = builder.end;
        score = builder.score;
        strand = builder.strand;
        phase = builder.phase;
        attributes = Collections.unmodifiableMap(builder.attributes);
    }

    public String getSeqId() {
        return seqId;
    }
    public String getSource() {
        return source;
    }
    public String getType() {
        return type;
    }
    public Integer getStart() {
        return start;
    }
    public Integer getEnd() {
        return end;
    }
    public Float getScore() {
        return score;
    }
    public Strand getStrand() {
        return strand;
    }
    public Integer getPhase() {
        return phase;
    }
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public List<String> getAttribute(String tag) {
        return attributes.getOrDefault(tag, Collections.emptyList());
    }

    public String getAttributeFirstValue(String tag) {
        return Optional.ofNullable(attributes.get(tag))
            .filter(ids -> ids.size() > 0)
            .map(ids -> ids.get(0))
            .orElse(null);
    }

    public String getId() {
        return getAttributeFirstValue("ID");
    }

    public String getParentId() {
        return getAttributeFirstValue("Parent");
    }
 
    public String getGenomicLocation() {
        return getSeqId() + ":" + getStart() + ".." + getEnd();
    }

    @Override
    public String toString() {
        return getType() + "[" + getGenomicLocation() + "]";
    }
}
