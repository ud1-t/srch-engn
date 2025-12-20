package com.search.processing;

import com.search.model.Token;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[a-zA-Z]{2,}\\b");

    public List<Token> tokenize(String text) {
        List<Token> tokens = new java.util.ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text.toLowerCase());

        int position = 0;

        while(matcher.find()) {
            tokens.add(
                new Token(
                    matcher.group(),
                    position++,
                    matcher.start()
                )
            );
        }

        return tokens;
    }
}
