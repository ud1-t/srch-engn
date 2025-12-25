package com.search.shell;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.search.segment.Segment;
import com.search.segment.SegmentManager;
import com.search.storage.DocumentRepository;
import com.search.storage.IndexRepository;
import com.search.storage.SegmentRepository;
import com.search.storage.impl.DocumentRepositoryImpl;
import com.search.storage.impl.IndexRepositoryImpl;
import com.search.storage.impl.SegmentRepositoryImpl;

public class SearchShell {

    private final SegmentManager segmentManager = new SegmentManager();

    private SegmentRepository segmentRepository;
    private DocumentRepository documentRepository;
    private IndexRepository indexRepository;

    private final DocumentFetcher fetcher = new WikipediaFetcher();
    private final TextProcessor processor = new TextProcessor();
    private final QueryParser queryParser = new QueryParser();
    private final Ranker ranker = new TfIdfRanker();
    private final SnippetGenerator snippetGenerator = new SnippetGenerator();

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

            segmentRepository = new SegmentRepositoryImpl(connection);
            documentRepository = new DocumentRepositoryImpl(connection);
            indexRepository = new IndexRepositoryImpl(connection);

            segmentManager.clear();

            Map<Integer, Document> canonicalDocs = documentRepository.loadCanonicalDocuments();

            for(int segmentId : segmentRepository.loadActiveSegmentIds()) {
                Segment segment = new Segment(segmentId);
                InvertedIndex index = indexRepository.loadIndexForSegment(segmentId);
                segment.setIndex(index);
                Map<Integer, Integer> segmentDocs = documentRepository.loadSegmentDocuments(segmentId);

                for(var entry : segmentDocs.entrySet()) {
                    int docId = entry.getKey();
                    int canonicalId = entry.getValue();
                    Document doc = canonicalDocs.get(canonicalId);
                    if(doc == null) {
                        throw new IllegalStateException("Missing canonical document " + canonicalId);
                    }
                    segment.addDocument(docId, doc);
                }
                segmentManager.addSegment(segment);
            }
            System.out.println("Loaded " + segmentManager.getSegments().size() + " segments.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize", e);
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
            else if (input.startsWith("merge ")) {
                String[] parts = input.split("\\s+");
                if(parts.length != 3) {
                    System.out.println("Usage: merge <segA> <segB>");
                    return;
                }
                mergeSegments(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } else
                System.out.println("Unknown command. Type 'help' to see available commands.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seed(String pageKey) {
        Document doc = fetcher.fetch(pageKey);
        Map<String, Integer> canonicalIds = documentRepository.saveCanonicalDocuments(List.of(doc));
        int canonicalId = canonicalIds.get(doc.getUrl());

        if(segmentRepository.isCanonicalDocInActiveSegment(canonicalId)) {
            System.out.println("Document already indexed: " + doc.getUrl());
            return;
        }

        int segmentId = segmentRepository.createSegment();
        Segment segment = new Segment(segmentId);

        Map<Integer, Integer> mapping = documentRepository.mapToSegment(segmentId, List.of(canonicalId));

        Integer docId = mapping.get(canonicalId);

        List<Token> tokens = processor.process(doc.getContent());
        indexRepository.appendDocument(segmentId, docId, canonicalId, tokens);

        segment.addDocument(docId, doc, tokens);
        segmentManager.addSegment(segment);

        System.out.println("Created segment " + segmentId + " | docId=" + docId + " | " + doc.getTitle());
    }

    private void seedFile(String path) {
        try {
            List<String> pageKeys = Files.readAllLines(Path.of(path))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

            List<Document> docs = pageKeys.stream().map(fetcher::fetch).toList();
            Map<String, Integer> canonicalIds = documentRepository.saveCanonicalDocuments(docs);
            List<Integer> newCanonicalIds = canonicalIds.values().stream()
                .filter(id -> !segmentRepository.isCanonicalDocInActiveSegment(id))
                .toList();

            if(newCanonicalIds.isEmpty()) {
                System.out.println("No new documents to index.");
                return;
            }

            int segmentId = segmentRepository.createSegment();
            Segment segment = new Segment(segmentId);

            Map<Integer, Integer> mapping = documentRepository.mapToSegment(segmentId, newCanonicalIds);

            for(Document doc : docs) {
                int canonicalId = canonicalIds.get(doc.getUrl());
                Integer docId = mapping.get(canonicalId);
                if(docId == null) continue;

                List<Token> tokens = processor.process(doc.getContent());
                indexRepository.appendDocument(segmentId, docId, canonicalId, tokens);
                segment.addDocument(docId, doc, tokens);
            }

            segmentManager.addSegment(segment);
            System.out.println("Created segment " + segmentId + " | docs=" + segment.getDocuments().size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed file: " + path, e);
        }
    }

    private void search(String query) {
        List<String> terms = queryParser.parse(query);
        List<SearchResult> results = new ArrayList<>();

        for(Segment segment : segmentManager.getSegments()) {
            InvertedIndex index = segment.getIndex();
            QueryExecutor executor = new QueryExecutor(index);
            Set<Integer> matchingDocIds = executor.execute(terms);

            if(matchingDocIds.isEmpty()) continue;

            Map<Integer, Double> scores = ranker.rank(terms, index, segment.getDocuments().size());

            for(int docId : matchingDocIds) {
                double score = scores.getOrDefault(docId, 0.0);
                results.add(new SearchResult(segment, docId, score));
            }
        }

        // results.sort(Comparator.comparingDouble((SearchResult r) -> r.score).reversed());
        results.sort(Comparator.comparingDouble(r -> -r.score));

        ConsoleUI.header("SEARCH RESULTS");

        for(SearchResult r : results) {
            printResult(r.segment, r.docId, r.score, terms);
        }
    }

    private void printResult(Segment segment, int docId, double score, List<String> terms) {
        Document doc = segment.getDocuments().get(docId);

        ConsoleUI.kv("Segment", String.valueOf(segment.getSegmentId()));
        ConsoleUI.kv("Title", doc.getTitle());
        ConsoleUI.kv("URL", doc.getUrl());
        ConsoleUI.kv("Score", String.format("%.4f", score));

        for (String term : terms) {
            Posting posting = segment.getIndex().getPostings(term).get(docId);
            if(posting == null) continue;

            for(int i = 0; i < posting.getOffsets().size(); i++) {
                String snippet = snippetGenerator.generate(
                    doc.getContent(),
                    posting.getOffsets().get(i),
                    term
                );
                System.out.println("SNIPPET [" + term + " #" + (i + 1) + "]: " + snippet);
            }
        }

        ConsoleUI.line();
    }

    private void mergeSegments(int segAId, int segBId) {
        int newSegmentId = segmentRepository.createSegment();
        Segment merged = segmentManager.merge(segAId, segBId, newSegmentId);

        List<Integer> canonicalIds = new ArrayList<>();
        for(Document d : merged.getDocuments().values()) {
            canonicalIds.add(
                documentRepository
                    .saveCanonicalDocuments(List.of(d))
                    .get(d.getUrl())
            );
        }

        Map<Integer, Integer> mapping = documentRepository.mapToSegment(newSegmentId, canonicalIds);

        for(var entry : mapping.entrySet()) {
            int canonicalId = entry.getKey();
            int docId = entry.getValue();
            Document doc = documentRepository.loadCanonicalDocuments().get(canonicalId);

            List<Token> tokens = processor.process(doc.getContent());
            indexRepository.appendDocument(newSegmentId, docId, canonicalId, tokens);
        }

        segmentRepository.deactivateSegment(segAId);
        segmentRepository.deactivateSegment(segBId);

        initializeFromRepo();

        System.out.println("Merged segments " + segAId +" + " + segBId +" → " + newSegmentId);
    }

    private void help() {
        System.out.println();
        System.out.println("Available commands:");
        System.out.println();
        System.out.println("  load");
        System.out.println("      Reload documents and index from database");
        System.out.println();
        System.out.println("  seed <page-key>");
        System.out.println("      Fetch and index a single Wikipedia page, eg: seed potato");
        System.out.println();
        System.out.println("  seed-file <path>");
        System.out.println("      Seed multiple Wikipedia pages from a file (one key per line)");
        System.out.println();
        System.out.println("  search <query>");
        System.out.println("      Search indexed documents");
        System.out.println();
        System.out.println("  merge <segA> <segB>");
        System.out.println("      Merge two segments");
        System.out.println();
        System.out.println("  help");
        System.out.println("      Show this help message");
        System.out.println();
        System.out.println("  exit");
        System.out.println("      Exit the shell");
        System.out.println();
    }

    private static class SearchResult {
        final Segment segment;
        final int docId;
        final double score;

        SearchResult(Segment segment, int docId, double score) {
            this.segment = segment;
            this.docId = docId;
            this.score = score;
        }
    }
}
