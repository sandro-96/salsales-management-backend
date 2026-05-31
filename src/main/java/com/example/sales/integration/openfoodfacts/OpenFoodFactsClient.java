package com.example.sales.integration.openfoodfacts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class OpenFoodFactsClient {

    private static final String SEARCH_BASE =
            "https://world.openfoodfacts.org/api/v2/search";
    private static final String VIETNAM_TAG = "vietnam";
    private static final String FIELDS =
            "code,product_name,product_name_vi,categories,brands,image_front_url,image_url,"
                    + "ingredients_text_vi,ingredients_text,quantity";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private final ObjectMapper objectMapper;

    public OpenFoodFactsClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param page 1-based (OFF API)
     */
    public Optional<JsonNode> searchVietnamProducts(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        String url = SEARCH_BASE
                + "?countries_tags_en=" + encode(VIETNAM_TAG)
                + "&page=" + safePage
                + "&page_size=" + safeSize
                + "&fields=" + encode(FIELDS)
                + "&sort_by=unique_scans_n";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "SalSales-Catalog/1.0 (admin Vietnam browse)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("OFF search Vietnam HTTP {}", response.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.path("products").isArray()) {
                return Optional.empty();
            }
            return Optional.of(root);
        } catch (Exception e) {
            log.warn("OFF search Vietnam failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
