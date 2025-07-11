// File: src/main/java/com/example/sales/util/MessageService.java
package com.example.sales.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String get(String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }
}
