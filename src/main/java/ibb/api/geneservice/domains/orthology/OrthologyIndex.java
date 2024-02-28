package ibb.api.geneservice.domains.orthology;

import java.util.List;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import co.elastic.clients.elasticsearch.ingest.Processor;
import ibb.api.geneservice.domains.synonym.SynonymIndex;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrthologyIndex extends ESSourceIndex<Orthology> {

    @Inject
    SynonymIndex synonymIndex;

    public OrthologyIndex() {
        super("orthology");
    }

    @Override
    protected TypeMapping getTypeMapping() {
        return TypeMapping.of(t -> t
            .properties("source", p -> p.keyword(k -> k))
            .properties("group", p -> p.keyword(k -> k))
            .properties("orthologs", p -> p.object(o -> o
                .properties("gene", p2 -> p2.keyword(k -> k))
                .properties("species", p2 -> p2.keyword(k -> k))
                .properties("synonyms", p2 -> p2.object(o2 -> o2
                    .properties("type", p3 -> p3.keyword(k -> k))
                    .properties("synonym", p3 -> p3.text(t3 -> t3.analyzer("synonym")))
                ))
            ))
        );
    }

    @Override
    protected List<Processor> getPipelineProcessors() {
        return List.of(
            Processor.of(pr -> pr.foreach(fe -> fe
                .field("orthologs")
                .processor(pr2 -> pr2.enrich(e -> e
                    .policyName(synonymIndex.getGene2SynonymsPolicyName())
                    .field("_ingest._value.gene")
                    .targetField("_ingest._value.synonyms")
                    .maxMatches(128)
                ))
            )),
            Processor.of(pr -> pr.script(s -> s.inline(script -> script.source("""
                ctx.orthologs = ctx.orthologs.stream().map(o -> {
                    o.synonyms = o.synonyms.stream()
                        .filter(s -> s.species.equals(o.species))
                        .filter(s -> !'PROTEIN'.equals(s.type))
                        .filter(s -> !'TRANSCRIPT'.equals(s.type))
                        .collect(Collectors.toList());
                    return o;
                }).collect(Collectors.toList());
            """
            )))),
            Processor.of(pr -> pr.foreach(f -> f
                .field("orthologs")
                .processor(pr2 -> pr2.foreach(f2 -> f2
                    .field("_ingest._value.synonyms")
                    .processor(pr3 -> pr3.remove(r -> r
                        .field("_ingest._value.species", "_ingest._value.gene")
                    ))
                ))
            ))
        );
    }

    @Override
    protected IndexSettingsAnalysis getAnalysis() {
        return IndexSettingsAnalysis.of(a -> a
            .analyzer("synonym", an -> an
                .custom(c -> c
                    .tokenizer("whitespace")
                    .filter("lowercase")
                )
            )
        );
    }
}
