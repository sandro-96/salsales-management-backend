package com.example.sales.util;

import com.example.sales.constant.ApiCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves {@link ApiCode} to localized text via {@code messages_*.properties}.
 * Falls back to the enum's default message when no bundle entry exists.
 */
@Component
public class ApiMessages {

    private static MessageSource messageSource;

    @Autowired
    public ApiMessages(MessageSource ms) {
        ApiMessages.messageSource = ms;
    }

    public static String resolve(ApiCode code) {
        return resolve(code, LocaleContextHolder.getLocale(), null);
    }

    public static String resolve(ApiCode code, Map<String, ?> params) {
        return resolve(code, LocaleContextHolder.getLocale(), params);
    }

    public static String resolve(ApiCode code, Locale locale, Map<String, ?> params) {
        if (messageSource == null || code == null) {
            return code != null ? code.getMessage() : "";
        }
        return messageSource.getMessage(
                code.getMessageKey(),
                null,
                code.getMessage(),
                locale != null ? locale : Locale.getDefault());
    }
}
