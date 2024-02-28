package ibb.api.geneservice.search;

import com.fasterxml.jackson.annotation.JsonValue;

public class SuggestItem {
    private String synonym;

    public void setSynonym(String synonym) {
        this.synonym = synonym;
    }

    @JsonValue
    public String getSynonym() {
        return synonym;
    }
}
