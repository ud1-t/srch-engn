package com.search.shell;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.search.console.ConsoleUI;
import com.search.fetch.DocumentFetcher;
import com.search.fetch.impl.WikipediaFetcher;
import com.search.index.InvertedIndex;
import com.search.index.Posting;
import com.search.model.Document;
import com.search.processing.TextProcessor;
import com.search.query.QueryExecutor;
import com.search.query.QueryParser;
import com.search.query.SnippetGenerator;
import com.search.ranking.Ranker;
import com.search.ranking.TfIdfRanker;
import com.search.storage.DocumentRepository;
import com.search.storage.IndexRepository;
import com.search.storage.impl.DocumentRepositoryImpl;
import com.search.storage.impl.IndexRepositoryImpl;

public class SearchShell {
    
    private InvertedIndex index;
    private Map<Integer, Document> documents;
    private int nextDocId;

    private final DocumentFetcher fetcher = new WikipediaFetcher();
    private final TextProcessor processor = new TextProcessor();
    private final QueryParser queryParser = new QueryParser();
    private final Ranker ranker = new TfIdfRanker();
    private final SnippetGenerator snippetGenerator = new SnippetGenerator();

    private DocumentRepository documentRepository;
    private IndexRepository indexRepository;

    public void run() {
        initializeFromRepo();
        repl();
    }

    private void initializeFromRepo() {
        try {
            Connection connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/search",
                "search",
                "search"
            );

            documentRepository = new DocumentRepositoryImpl(connection);
            indexRepository = new IndexRepositoryImpl(connection);

            documents = documentRepository.loadDocuments();
            index = indexRepository.loadIndex();

            if(documents.isEmpty()) {
                nextDocId = 1;
            } else {
                nextDocId = Collections.max(documents.keySet()) + 1;
            }

            System.out.println("Loaded " + documents.size() + " documents into memory.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize from repository", e);
        }
    }

    private void repl() {
        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.print("\nsearch-engine > ");
            String input = scanner.nextLine().trim();

            if(input.equals("exit")) {
                System.out.println("Exiting.");
                break;
            }

            handleInput(input);
        }

        scanner.close();
    }

    private void handleInput(String input) {
        try {
            if(input.equals("load"))
                initializeFromRepo();
            else if(input.startsWith("seed "))
                seed(input.substring(5));
            else if(input.startsWith("search "))
                search(input.substring(7));
            else if(input.equals("persist"))
                persist();
            else
                System.out.println("Unknown command: " + input);
        } catch (Exception e) {
            System.out.println("Error handling input: " + e.getMessage());
        }
    }

    private void seed(String url) {
        Document doc = fetcher.fetch(url);
        int docId = nextDocId++;

        documents.put(docId, doc);
        index.addDocument(
            docId, 
            processor.process(doc.getContent())
        );

        persist();

        System.out.println("Seeded document ID " + docId + " | " + doc.getTitle());
    }

    private void search(String query) {
        List<String> terms = queryParser.parse(query);
        
        QueryExecutor executor = new QueryExecutor(index);
        Set<Integer> matchingDocIds = executor.execute(terms);

        Map<Integer, Double> rankedResults = ranker.rank(
            terms,
            index,
            documents.size()
        );

        ConsoleUI.header("SEARCH RESULTS");
        rankedResults.entrySet().stream()
            .filter(e -> matchingDocIds.contains(e.getKey()))
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .forEach(e -> printResult(
                e.getKey(),
                e.getValue(),
                terms
            ));
    }

    private void printResult(int docId, double score, List<String> terms) {
        Document doc = documents.get(docId);

        ConsoleUI.kv("Title", doc.getTitle());
        ConsoleUI.kv("URL", doc.getUrl());
        ConsoleUI.kv("Score", String.format("%.4f", score));

        for (String term : terms) {
            Posting posting = index.getPostings(term).get(docId);
            if (posting != null && !posting.getOffsets().isEmpty()) {
                int offset = posting.getOffsets().get(0);
                String snippet = snippetGenerator.generate(doc.getContent(), offset, term);
                System.out.println("SNIPPET [" + term + "]: " + snippet);
            }
        }
        ConsoleUI.line();
    }

    private void persist() {
        documentRepository.saveDocuments(documents);
        indexRepository.saveIndex(index);
        System.out.println("State persisted");
    }
}
