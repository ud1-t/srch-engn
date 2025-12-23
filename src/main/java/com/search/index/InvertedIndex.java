package com.search.index;

import com.search.model.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class InvertedIndex {
    private final Map<String, Map<Integer, Posting>> index = new HashMap<>();

    public void addTerm(String term, int docId, int position, int offset) {
        index.computeIfAbsent(term, t -> new HashMap<>())
                .computeIfAbsent(docId, d -> new Posting())
                .addOccurrence(position, offset);
    }

    public void addDocument(int docId, List<Token> tokens) {
        tokens.forEach(token -> 
            addTerm(
                token.getTerm(),
                docId,
                token.getPosition(),
                token.getOffset()
            )
        );
    }

    public Map<Integer, Posting> getPostings(String term) {
        return index.getOrDefault(term, Map.of());
    }

    public Map<String, Map<Integer, Posting>> getIndex() { return index; }
}
