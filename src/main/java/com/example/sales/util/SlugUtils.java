package com.example.sales.util;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

public class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^a-zA-Z0-9-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return randomSlug();
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // remove accents

        String slug = WHITESPACE.matcher(normalized).replaceAll("-")
                .replaceAll("[^a-zA-Z0-9-]", "")
                .toLowerCase();

        if (slug.isBlank()) {
            return randomSlug();
        }

        return slug;
    }

    public static String uniqueSlug(String input) {
        return toSlug(input) + "-" + randomSlug();
    }

    private static String randomSlug() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}

