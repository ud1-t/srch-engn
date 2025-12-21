package com.search.query;

import java.util.List;

import com.search.model.Token;
import com.search.processing.TextProcessor;

public class QueryParser {
    private final TextProcessor textProcessor = new TextProcessor();

    public List<String> parse(String query) {
        return textProcessor.process(query)
                            .stream()
                            .map(Token::getTerm)
                            .toList();
    }
}
