package ibb.api.geneservice.domains.orthology;

import java.io.IOException;
import java.io.UncheckedIOException;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrthologyIndex extends ESSourceIndex<Orthology> {

    @Inject
    SynonymIndex synonymIndex;

    private String pipelineName;

    public OrthologyIndex() {
        super("orthology");
    }

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        pipelineName = getESHelper().getESName("orthology-pipeline");
    }

    @Override
    public void delete() {
        super.delete();
        getESHelper().deletePipelineIgnoreUnavailable(pipelineName);
    }

    @Override
    public void setup() {
        try {
            getESClient().ingest().putPipeline(p -> p
                .id(pipelineName)
                .processors(pr -> pr.foreach(fe -> fe
                    .field("orthologs")
                    .processor(pr2 -> pr2.enrich(e -> e
                        .policyName(synonymIndex.getGene2SynonymsPolicyName())
                        .field("_ingest._value.gene")
                        .targetField("_ingest._value.synonyms")
                        .maxMatches(128)
                    ))
                ))
                .processors(pr -> pr.script(s -> s.inline(script -> script.source("""
                    ctx.orthologs = ctx.orthologs.stream().map(o -> {
                        o.synonyms = o.synonyms.stream()
                            .filter(s -> s.species.equals(o.species))
                            .filter(s -> !'PROTEIN'.equals(s.type))
                            .filter(s -> !'TRANSCRIPT'.equals(s.type))
                            .collect(Collectors.toList());
                        return o;
                    }).collect(Collectors.toList());
                """
                ))))
                .processors(pr -> pr.foreach(f -> f
                    .field("orthologs")
                    .processor(pr2 -> pr2.foreach(f2 -> f2
                        .field("_ingest._value.synonyms")
                        .processor(pr3 -> pr3.remove(r -> r
                            .field("_ingest._value.species", "_ingest._value.gene")
                        ))
                    ))
                ))
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        super.setup();
    }

    @Override
    protected TypeMapping getTypeMapping() {
        return null;
    }

    @Override
    protected String getDefaultPipeline() {
        return pipelineName;
    }
}
