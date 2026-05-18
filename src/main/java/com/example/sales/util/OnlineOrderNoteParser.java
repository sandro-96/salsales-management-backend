package com.example.sales.util;

import org.springframework.util.StringUtils;

/**
 * Parse {@code Order.note} do storefront ghi bởi {@code StorefrontService#composeShippingNote}.
 */
public final class OnlineOrderNoteParser {

    private OnlineOrderNoteParser() {
    }

    public record ParsedOnlineNote(
            String shippingAddress,
            String customerEmail,
            String customerNote) {
    }

    public static ParsedOnlineNote parse(String note) {
        if (!StringUtils.hasText(note)) {
            return new ParsedOnlineNote(null, null, null);
        }
        String shippingAddress = null;
        String customerEmail = null;
        String customerNote = null;

        for (String line : note.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Địa chỉ:")) {
                shippingAddress = trimmed.substring("Địa chỉ:".length()).trim();
            } else if (trimmed.startsWith("Email:")) {
                customerEmail = trimmed.substring("Email:".length()).trim();
            } else if (trimmed.startsWith("Ghi chú:")) {
                customerNote = trimmed.substring("Ghi chú:".length()).trim();
            }
        }

        return new ParsedOnlineNote(
                StringUtils.hasText(shippingAddress) ? shippingAddress : null,
                StringUtils.hasText(customerEmail) ? customerEmail : null,
                StringUtils.hasText(customerNote) ? customerNote : null);
    }
}
