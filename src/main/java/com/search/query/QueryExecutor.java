package com.search.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.search.index.InvertedIndex;

public class QueryExecutor {
    private final InvertedIndex index;

    public QueryExecutor(InvertedIndex index) {
        this.index = index;
    }

    public Set<Integer> execute(List<String> terms) {
        Set<Integer> resultDocIds = new HashSet<>();

        terms.forEach(term -> {
            Set<Integer> docIds = index.getPostings(term).keySet();
            if(resultDocIds.isEmpty()) {
                resultDocIds.addAll(docIds);
            } else {
                resultDocIds.retainAll(docIds);
            }
        });

        return resultDocIds;
    }
}
