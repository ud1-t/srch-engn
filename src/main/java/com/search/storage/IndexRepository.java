package com.search.storage;

import java.util.List;

import com.search.index.InvertedIndex;
import com.search.model.Token;

public interface IndexRepository {
    void appendDocument(int docId, List<Token> tokens);
    InvertedIndex loadIndex();
}
