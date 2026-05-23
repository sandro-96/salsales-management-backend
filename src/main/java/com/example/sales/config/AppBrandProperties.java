package com.example.sales.config;

import com.example.sales.util.Utf8TextUtil;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "app.brand")
public class AppBrandProperties {

    private String name = "S\u1ed5 thu chi";

    public void setName(String name) {
        this.name = Utf8TextUtil.fixUtf8Mojibake(name);
    }
}
