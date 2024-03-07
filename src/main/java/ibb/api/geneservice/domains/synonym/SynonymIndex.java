package ibb.api.geneservice.domains.synonym;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
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
			.properties("synonym", p -> p.searchAsYouType(s -> s
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

	public SynonymSuggestResult suggest(String query, List<String> searchAfter) {
		var requestBuilder = new SearchRequest.Builder()
			.sort(s -> s.field(f -> f.field("synonym.keyword").order(SortOrder.Asc)))
			.query(q -> q
				.matchPhrasePrefix(m -> m
					.field("synonym")
					.query(query)
				)
			);
        if (searchAfter != null && !searchAfter.isEmpty()) {
            requestBuilder.searchAfter(searchAfter.stream().map(FieldValue::of).toList());
        }
        return SynonymSuggestResult.of(search(requestBuilder, 20));
	}

	public List<Synonym> findBySynonym(String synonym) {
		var requestBuilder = new SearchRequest.Builder()
			.query(q -> q.term(t -> t.field("synonym.keyword").value(synonym)));
		return search(requestBuilder).hits().hits().stream().map(h -> h.source()).toList();
	}
}
