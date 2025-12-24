package com.search.shell;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
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
import com.search.model.Token;
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
            input = input.trim();

            if (input.equals("help"))
                help();
            else if (input.equals("load"))
                initializeFromRepo();
            else if (input.startsWith("seed-file "))
                seedFile(input.substring(10).trim());
            else if (input.startsWith("seed "))
                seed(input.substring(5).trim());
            else if (input.startsWith("search "))
                search(input.substring(7).trim());
            else
                System.out.println("Unknown command. Type 'help' to see available commands.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seed(String url) {
        Document doc = fetcher.fetch(url);

        Map<String, Integer> ids =
            documentRepository.saveDocuments(List.of(doc));

        Integer docId = ids.get(doc.getUrl());
        if (docId == null) {
            System.out.println("Document already exists: " + url);
            return;
        }

        documents.put(docId, doc);
        List<Token> tokens = processor.process(doc.getContent());
        index.addDocument(docId, tokens);
        indexRepository.appendDocument(docId, tokens);

        System.out.println("Seeded document ID " + docId + " | " + doc.getTitle());
    }

    private void seedFile(String path) {
        try {
            List<String> urls = Files.readAllLines(Path.of(path))
                                    .stream()
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .toList();

            List<Document> docs = new ArrayList<>();
            for (String url : urls) {
                docs.add(fetcher.fetch(url));
            }

            Map<String, Integer> ids =
                documentRepository.saveDocuments(docs);

            for (Document doc : docs) {
                Integer docId = ids.get(doc.getUrl());
                if (docId == null) {
                    System.out.println("Already exists: " + doc.getUrl());
                    continue;
                }

                documents.put(docId, doc);
                List<Token> tokens = processor.process(doc.getContent());
                index.addDocument(docId, tokens);
                indexRepository.appendDocument(docId, tokens);

                System.out.println("Seeded " + docId + " | " + doc.getTitle());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                for (int i = 0; i < posting.getOffsets().size(); i++) {
                    int offset = posting.getOffsets().get(i);
                    String snippet =
                        snippetGenerator.generate(doc.getContent(), offset, term);

                    System.out.println(
                        "SNIPPET [" + term + " #" + (i + 1) + "]: " + snippet
                    );
                }
            }
        }
        ConsoleUI.line();
    }

    private void help() {
        System.out.println();
        System.out.println("Available commands:");
        System.out.println();
        System.out.println("  load");
        System.out.println("      Reload documents and index from database");
        System.out.println();
        System.out.println("  seed <wiki-url>");
        System.out.println("      Fetch and index a single Wikipedia page");
        System.out.println();
        System.out.println("  seed-file <path>");
        System.out.println("      Seed multiple Wikipedia URLs from a file (one URL per line)");
        System.out.println();
        System.out.println("  search <query>");
        System.out.println("      Search indexed documents");
        System.out.println();
        System.out.println("  help");
        System.out.println("      Show this help message");
        System.out.println();
        System.out.println("  exit");
        System.out.println("      Exit the shell");
        System.out.println();
    }
}
