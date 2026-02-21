package com.search.fetch.impl;

import com.search.fetch.DocumentFetcher;
import com.search.model.Document;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WikipediaFetcher implements DocumentFetcher {

    private static final String WIKI_EXTRACT_API =
        "https://en.wikipedia.org/w/api.php?action=query&prop=extracts&explaintext=true&format=json&titles=";

    @Override
    public Document fetch(String pageKey) {
        try {
            String apiUrl = WIKI_EXTRACT_API + URLEncoder.encode(pageKey, StandardCharsets.UTF_8);

            HttpClient client = HttpClients.createDefault();
            HttpGet request = new HttpGet(apiUrl);

            request.addHeader("User-Agent", "wiki-search-engine");
            request.addHeader("Accept", "application/json");

            String response = client.execute(
                request,
                httpResponse ->
                    EntityUtils.toString(
                        httpResponse.getEntity(),
                        StandardCharsets.UTF_8
                    )
            );

            JSONObject json = new JSONObject(response);
            JSONObject pages = json.getJSONObject("query").getJSONObject("pages");

            // The API returns pages keyed by page ID — get the first (only) entry
            String pageId = pages.keys().next();
            if (pageId.equals("-1")) {
                throw new RuntimeException("Page not found: " + pageKey);
            }

            JSONObject page = pages.getJSONObject(pageId);
            String title = page.getString("title");
            String content = page.getString("extract");
            String canonicalUrl = "https://en.wikipedia.org/wiki/" +
                URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);

            return new Document(canonicalUrl, title, content);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Wikipedia page: " + pageKey, e);
        }
    }
}
