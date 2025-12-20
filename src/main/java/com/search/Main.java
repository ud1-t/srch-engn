package com.search;

import com.search.fetch.DocumentFetcher;
import com.search.fetch.impl.WikipediaFetcher;
import com.search.model.Document;

public class Main {
    public static void main(String[] args) {
        System.out.println("-------------");
        System.out.println("Hello, World!");
        System.out.println("-------------");

        DocumentFetcher fetcher = new WikipediaFetcher();

        Document doc = fetcher.fetch(
                "https://en.wikipedia.org/wiki/Vikkstar123"
        );

        System.out.println("URL: " + doc.getUrl());
        System.out.println("TITLE: " + doc.getTitle());
        System.out.println("CONTENT LENGTH: " + doc.getContent().length());
        System.out.println(doc.getContent());
    }
}


