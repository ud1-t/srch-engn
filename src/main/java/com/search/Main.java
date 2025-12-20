package com.search;

import java.util.List;

import com.search.fetch.DocumentFetcher;
import com.search.fetch.impl.WikipediaFetcher;
import com.search.model.Document;
import com.search.model.Token;
import com.search.processing.TextProcessor;

public class Main {
    public static void main(String[] args) {
        System.out.println("-------------");
        System.out.println("Hello, World!");
        System.out.println("-------------");

        DocumentFetcher fetcher = new WikipediaFetcher();

        Document doc = fetcher.fetch(
                "https://en.wikipedia.org/wiki/Emergency"
        );

        System.out.println("URL: " + doc.getUrl());
        System.out.println("TITLE: " + doc.getTitle());
        System.out.println("CONTENT LENGTH: " + doc.getContent().length());
        System.out.println(doc.getContent());

        System.out.println("-------------");
        System.out.println("-------------");

        TextProcessor processor = new TextProcessor();
        List<Token> tokens = processor.process(doc.getContent());

        System.out.println("TOTAL TOKENS: " + tokens.size());
        tokens.stream().forEach(token -> 
            System.out.println(
                "TERM: " + token.getTerm() +
                ", POSITION: " + token.getPosition() +
                ", OFFSET: " + token.getOffset()
            )
        );
    }
}


