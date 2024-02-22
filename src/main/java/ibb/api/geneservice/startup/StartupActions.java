package ibb.api.geneservice.startup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.JsonData;
import ibb.api.geneservice.domains.ortholog.OrthologParser;
import ibb.api.geneservice.index.DocumentSource;
import ibb.api.geneservice.index.IndexManager;
import ibb.api.geneservice.index.IndexType;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class StartupActions {

    @ConfigProperty(name = "geneservice.data-dir")
    String dataDir;

    @Inject
    SpeciesLoader speciesLoader;

    @Inject
    IndexManager indexManager;

    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "geneservice.elasticsearch.force-recreate-enriched-index", defaultValue = "false")
    boolean forceRecreateEnrichedIndex;

    @ConfigProperty(name = "geneservice.elasticsearch.force-reload-orthologs", defaultValue = "false")
    boolean forceReloadOrthologs;

    @ConfigProperty(name = "geneservice.elasticsearch.force-reload-species", defaultValue = "false")
    boolean forceReloadSpecies;

    void init(@Observes StartupEvent event) throws IOException {
        if (forceReloadSpecies) {
            indexManager.deleteAll(IndexType.GENOMIC_LOCATION);
            indexManager.deleteAll(IndexType.SEQUENCE);
            indexManager.deleteAll(IndexType.SYNONYM);
        }
        subDirs("species").forEach(speciesDir -> speciesLoader.load(speciesDir.getName(), speciesDir));
        Optional<String> pipeline = createOrthologIngestPipeline();

        if (forceReloadOrthologs) {
            indexManager.deleteAll(IndexType.ORTHOLOG);
        }
        subDirs("orthologs").forEach(orthologDir -> {
            File[] files = orthologDir.listFiles(File::isFile);
            String orthoSource = orthologDir.getName();
            var docSources = Arrays.stream(files)
                .map(file -> new DocumentSource<>(file, new OrthologParser(orthoSource), pipeline))
                .toList();
            indexManager.loadAllIfNotExists(docSources, IndexType.ORTHOLOG);
        });
        Log.info("All data loaded");
    }

    private Stream<File> subDirs(String dir) {
        return Arrays.stream(Path.of(dataDir, dir).toFile().listFiles(File::isDirectory));
    }

    private Optional<String> createOrthologIngestPipeline() throws IOException {
        if (!indexManager.exists(IndexType.SYNONYM)) {
            return Optional.empty();
        }

        String policyName = indexManager.normalizeName("synonym2gene");
        String pipelineName = indexManager.normalizeName("add_gene_to_ortholog");

        if (forceRecreateEnrichedIndex) {
            try {
                esClient.ingest().deletePipeline(p -> p.id(pipelineName));
            } catch (ElasticsearchException e) {
                if (e.status() != 404) {
                    throw e;
                }
            }
            try {
                esClient.enrich().deletePolicy(p -> p.name(policyName));
            } catch (ElasticsearchException e) {
                if (e.status() != 404) {
                    throw e;
                }
            }
        }

        boolean policyExists = esClient.enrich().getPolicy(p -> p.name(policyName)).policies().size() > 0;
        if (!policyExists) {
            esClient.enrich().putPolicy(p -> p
                .name(policyName)
                .match(m -> m
                    .indices(indexManager.getIndexName(IndexType.SYNONYM))
                    .matchField("value")
                    .enrichFields("gene")
                ));
            esClient.indices().refresh(r -> r
                .index(indexManager.getIndexName(IndexType.SYNONYM))
            );
            esClient.enrich().executePolicy(p -> p
                .name(policyName)
                .waitForCompletion(true)
            );
        }

        esClient.ingest().putPipeline(p -> p
            .id(pipelineName)
            .description("Enriches ortholog documents with current gene name")
            .processors(pr -> pr
                .enrich(e -> e
                    .policyName(policyName)
                    .field("ortholog")
                    .targetField("gene_enriched")
                ))
            .processors(pr -> pr
                .set(s -> s
                    .field("gene")
                    .value(JsonData.of("{{{gene_enriched.gene}}}"))
                ))
            .processors(pr -> pr
                .set(s -> s
                    .field("gene")
                    .value(JsonData.of("{{{ortholog}}}"))
                    .if_("ctx.gene == ''")
            ))
            .processors(pr -> pr
                .remove(r -> r
                    .field("gene_enriched")
                    .ignoreFailure(true)
                ))
        );
        return Optional.of(pipelineName);
    }
}
