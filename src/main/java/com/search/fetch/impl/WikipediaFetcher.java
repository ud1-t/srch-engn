package com.search.fetch.impl;

import com.search.fetch.DocumentFetcher;
import com.search.model.Document;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class WikipediaFetcher implements DocumentFetcher {

    private static final String WIKI_SUMMARY_API =
        "https://en.wikipedia.org/api/rest_v1/page/summary/";

    @Override
    public Document fetch(String pageKey) {
        try {
            String apiUrl = WIKI_SUMMARY_API + pageKey;

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

            String title = json.getString("title");
            String content = json.getString("extract");

            String canonicalUrl =
                json.getJSONObject("content_urls")
                    .getJSONObject("desktop")
                    .getString("page");

            return new Document(canonicalUrl, title, content);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Wikipedia page: " + pageKey, e);
        }
    }
}
