package com.search.processing;

import java.util.List;

import com.search.model.Token;

public class TextProcessor {
    private final Tokenizer tokenizer;
    private final StopWordFilter stopWordFilter;

    public TextProcessor() {
        this.tokenizer = new Tokenizer();
        this.stopWordFilter = new StopWordFilter();
    }

    public List<Token> process(String text) {
        List<Token> tokens = tokenizer.tokenize(text);
        return stopWordFilter.filter(tokens);
    }
}
