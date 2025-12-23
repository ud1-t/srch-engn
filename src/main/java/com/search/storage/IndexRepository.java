package com.search.storage;

import com.search.index.InvertedIndex;

public interface IndexRepository {
    void saveIndex(InvertedIndex index);
    InvertedIndex loadIndex();
}
