package ibb.api.geneservice.domains.synonym;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import ibb.api.geneservice.es.ESDocSourceProvider;
import ibb.api.geneservice.es.ESSourceIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SynonymIndex extends ESSourceIndex<Synonym> {

    @ConfigProperty(name = "geneservice.elasticsearch.delete-on-start.synonyms", defaultValue = "false")
    boolean shouldDeleteOnStart;

    @Inject
    ESDocSourceProvider<Synonym> docSourceProvider;

    public SynonymIndex() {
		super("synonym", Synonym.class);
	}

	@Override
	protected TypeMapping getTypeMapping() {
        return TypeMapping.of(t -> t
			.properties("synonym", p -> p.completion(s -> s
				.analyzer("whitespace_lowercase")
				.fields("keyword", k -> k.keyword(kk -> kk))
			))
		);
	}

	@Override
	protected IndexSettingsAnalysis getAnalysis() {
        return IndexSettingsAnalysis.of(a -> a
			.analyzer("whitespace_lowercase", getESHelper().getWhitespaceLowercaseAnalyzer())
        );
	}

	@Override
	protected boolean shouldDeleteOnStart() {
		return shouldDeleteOnStart;
	}

	public List<String> suggest(String query) {
		var requestBuilder = new SearchRequest.Builder()
			.suggest(s -> s
				.suggesters("suggest", ss -> ss
					.prefix(query)
					.completion(c -> c
						.field("synonym")
						.size(20)
						.skipDuplicates(true)
					)
				)
			);
        var response = search(requestBuilder, 0);
		return response.suggest().getOrDefault("suggest", List.of())
			.stream()
			.flatMap(s -> s.completion().options().stream())
			.map(o -> o.text())
			.toList();
	}

	public List<Synonym> findBySynonym(String synonym) {
		var requestBuilder = new SearchRequest.Builder()
			.query(q -> q.term(t -> t.field("synonym.keyword").value(synonym)));
		return search(requestBuilder, 1000).hits().hits().stream().map(h -> h.source()).toList();
	}

	public List<Synonym> findBySynonyms(List<String> synonyms) {
		var requestBuilder = new SearchRequest.Builder()
			.query(q -> q.terms(t -> t
				.field("synonym.keyword")
				.terms(tt -> tt.value(synonyms.stream().map(FieldValue::of).toList()))
			));
		return search(requestBuilder, 1000).hits().hits().stream().map(h -> h.source()).toList();
	}

	public List<Synonym> findByGenes(List<String> genes) {
		var requestBuilder = new SearchRequest.Builder()
			.query(q -> q.terms(t -> t
				.field("gene.keyword")
				.terms(tt -> tt.value(genes.stream().map(FieldValue::of).toList()))
			));
		return search(requestBuilder, 1000).hits().hits().stream().map(h -> h.source()).toList();
	}
}
