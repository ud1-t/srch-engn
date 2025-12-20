package com.search.fetch;

import com.search.model.Document;

public interface DocumentFetcher {
    Document fetch(String url);
}
