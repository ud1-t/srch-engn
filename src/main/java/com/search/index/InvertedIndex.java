package com.search.index;

import java.util.HashMap;
import java.util.Map;

public class InvertedIndex {
    private final Map<String, Map<Integer, Posting>> index = new HashMap<>();

    public void addTerm(String term, int docId, int position, int offset) {
        index.computeIfAbsent(term, t -> new HashMap<>())
                .computeIfAbsent(docId, d -> new Posting())
                .addOccurrence(position, offset);
    }

    public Map<Integer, Posting> getPostings(String term) {
        return index.getOrDefault(term, Map.of());
    }

    public Map<String, Map<Integer, Posting>> getIndex() { return index; }
}
