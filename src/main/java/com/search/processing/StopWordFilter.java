package com.search.processing;

import java.util.List;

import com.search.model.Token;
import static com.search.utils.AppConstants.STOP_WORDS;

public class StopWordFilter {
    public List<Token> filter(List<Token> tokens) {
        return tokens.stream()
                .filter(token -> !STOP_WORDS.contains(token.getTerm()))
                .toList();
    }
}
