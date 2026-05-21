package com.example.sales.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class FrontendCorsProperties {

    @Value("${app.fe.url}")
    private String frontendUrl;

    /**
     * Comma-separated origins/patterns, e.g.
     * https://myapp.vercel.app,https://*.vercel.app,http://localhost:5173
     * When empty, only {@link #frontendUrl} is used.
     */
    @Value("${app.fe.cors-origins:}")
    private String corsOrigins;

    public List<String> allowedOriginPatterns() {
        if (corsOrigins != null && !corsOrigins.isBlank()) {
            return Arrays.stream(corsOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return List.of(frontendUrl.trim());
    }
}
