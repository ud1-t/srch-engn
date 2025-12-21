package com.search.index;

import java.util.List;

import com.search.model.Token;

public class IndexBuilder {
    private final InvertedIndex index = new InvertedIndex();

    public void indexDocument(int docId, List<Token> tokens) {
        tokens.forEach(token ->
            index.addTerm(token.getTerm(), docId, token.getPosition(), token.getOffset())
        );
    }

    public InvertedIndex getIndex() { return index; }
}
