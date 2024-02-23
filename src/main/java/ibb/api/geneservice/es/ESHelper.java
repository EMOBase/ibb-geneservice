package ibb.api.geneservice.es;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ESHelper {
    
    @ConfigProperty(name = "geneservice.elasticsearch.index-prefix")
    Optional<String> indexPrefix;

    /**
     * Build an elasticsearch name from the given suffices.
     * The result would be in the form of {@code indexPrefix-suffix1-suffix2-...-suffixN}.
     * 
     * @param suffices the suffices to use
     * @return the normalized elasticsearch name
     */
    public String getESName(List<String> suffices) {
        List<String> prefices = indexPrefix.map(p -> List.of(p)).orElse(Collections.emptyList());
        List<String> words = Stream.of(prefices, suffices)
            .flatMap(List::stream)
            .filter(s -> !s.isBlank())
            .toList();
        return String.join("-", words).toLowerCase().replace(" ", "_");
    }

    /**
     * @see #getESName(List)
     */
    public String getESName(String... suffices) {
        return getESName(List.of(suffices));
    }

    /**
     * @see #getESName(List)
     */
    public String getESName(String suffix, List<String> suffices) {
        return getESName(Stream.concat(Stream.of(suffix), suffices.stream()).toList());
    }

    public String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}
