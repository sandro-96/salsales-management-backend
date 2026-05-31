package com.example.sales.integration.openfoodfacts;

import com.example.sales.dto.product.ProductCatalogOffBrowseItem;
import com.example.sales.util.CategoryUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OpenFoodFactsCatalogMapper {

    private OpenFoodFactsCatalogMapper() {
    }

    public static ProductCatalogOffBrowseItem toBrowseItem(JsonNode product) {
        if (product == null || product.isMissingNode()) {
            return null;
        }
        String code = text(product, "code");
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String name = firstNonBlank(
                text(product, "product_name_vi"),
                text(product, "product_name"),
                text(product, "abbreviated_product_name")
        );
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return ProductCatalogOffBrowseItem.builder()
                .barcode(code.trim())
                .name(name.trim())
                .category(resolveCategory(product))
                .description(buildDescription(product))
                .images(collectImages(product))
                .alreadyInCatalog(false)
                .build();
    }

    private static String resolveCategory(JsonNode product) {
        String categories = text(product, "categories");
        if (StringUtils.hasText(categories)) {
            String first = categories.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return CategoryUtils.normalize(first);
            }
        }
        JsonNode tags = product.path("categories_tags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                String t = tag.asText("");
                if (t.startsWith("en:")) {
                    String label = t.substring(3).replace('-', ' ');
                    if (StringUtils.hasText(label)) {
                        return CategoryUtils.normalize(label);
                    }
                }
            }
        }
        return CategoryUtils.normalize("Thực phẩm");
    }

    private static String buildDescription(JsonNode product) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, "Thương hiệu", text(product, "brands"));
        appendLine(sb, "Quy cách", text(product, "quantity"));
        String ingredients = firstNonBlank(
                text(product, "ingredients_text_vi"),
                text(product, "ingredients_text")
        );
        if (StringUtils.hasText(ingredients)) {
            if (ingredients.length() > 600) {
                ingredients = ingredients.substring(0, 600).trim() + "…";
            }
            appendLine(sb, "Thành phần", ingredients);
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static void appendLine(StringBuilder sb, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n");
        }
        sb.append(label).append(": ").append(value.trim());
    }

    private static List<String> collectImages(JsonNode product) {
        Set<String> urls = new LinkedHashSet<>();
        addImageUrl(urls, text(product, "image_front_url"));
        addImageUrl(urls, text(product, "image_url"));
        return urls.isEmpty() ? null : new ArrayList<>(urls);
    }

    private static void addImageUrl(Set<String> urls, String raw) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String u = raw.trim();
        if (u.startsWith("http://") || u.startsWith("https://")) {
            urls.add(u);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String s = v.asText("").trim();
        return s.isEmpty() ? null : s;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return null;
    }
}
