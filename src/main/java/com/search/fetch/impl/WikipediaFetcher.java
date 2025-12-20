package com.search.fetch.impl;

import com.search.fetch.DocumentFetcher;
import com.search.model.Document;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class WikipediaFetcher implements DocumentFetcher {

    @Override
    public Document fetch(String url) {
        try {
            URI uri = new URI(url);
            String title =
                    uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);

            String apiUrl =
                    "https://en.wikipedia.org/api/rest_v1/page/summary/" + title;

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
            String content = json.getString("extract");
            String pageTitle = json.getString("title");

            return new Document(url, pageTitle, content);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch page: " + url, e);
        }
    }
}
