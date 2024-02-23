package ibb.api.geneservice.es;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ComputedIndexManager {
    
    // @Inject
    // ESHelper esHelper;

    // @Inject
    // SourceIndexManager sourceIndexManager;

    // @Inject
    // ElasticsearchClient esClient;

    // public void delete(ComputedIndexType type) throws IOException {
    //     String name = getIndexName(type);
    //     esClient.indices().delete(d -> d.index(name).ignoreUnavailable(true));
    // }

    // public void recompute(ComputedIndexType type) throws IOException {
    //     delete(type);
    //     switch (type) {
    //         case ORTHOGROUP:
    //             transformOrthologs();
    //             break;
    //     }
    // }

    // private void transformOrthologs() throws IOException {
    //     String transformName = esHelper.getESName("ortholog2orthogroup");
    //     String sourceIndex = sourceIndexManager.getIndexName(SourceIndexType.ORTHOLOG);
    //     String targetIndex = getIndexName(ComputedIndexType.ORTHOGROUP);

    //     esClient.transform().putTransform(t -> t
    //         .transformId(transformName)
    //         .source(s -> s.index(sourceIndex))
    //         .dest(d -> d.index(targetIndex))
    //         .description("Group orthologs by orthogroup")
    //         .pivot(p -> p
    //             .groupBy("group", g -> g
    //                 .terms(term -> term
    //                     .field("group.keyword")))
    //             .aggregations("genes", a -> a
    //                 .scriptedMetric(m -> m
    //                     .initScript(script -> script.inline(i -> i.source("state.genes = new LinkedHashSet()")))
    //                     .mapScript(script -> script.inline(i -> i.source("state.genes.add(doc['gene.keyword'].value)")))
    //                     .combineScript(script -> script.inline(i -> i.source("return state.genes")))
    //                     .reduceScript(script -> script.inline(i -> i.source("def acc = []; for (state in states) { acc.addAll(state) } return acc")))
    //                 )
    //             )
    //         )
    //     );
    //     esClient.transform().startTransform(t -> t.transformId(transformName));
    // }

    // private String getIndexName(ComputedIndexType type) {
    //     return esHelper.getESName(type.name());
    // }
}
