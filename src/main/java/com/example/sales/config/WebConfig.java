// File: src/main/java/com/example/sales/config/WebConfig.java
package com.example.sales.config;

import com.example.sales.security.PlanInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private PlanInterceptor planInterceptor;

    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(planInterceptor)
                .addPathPatterns("/api/**"); // áp dụng cho mọi API
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/app-uploads/") // dev trên Windows
                .addResourceLocations("file:/var/www/app-uploads/"); // deploy Linux
    }

    /**
     * Nâng giới hạn maxSwallowSize của Tomcat lên 100MB.
     * Mặc định Tomcat chỉ cho phép "swallow" tối đa 2MB sau khi reject request,
     * dẫn đến lỗi "Failed to parse multipart servlet request" khi upload nhiều ảnh lớn.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            // -1 = không giới hạn, hoặc đặt giá trị cụ thể (bytes): 100 * 1024 * 1024 = 100MB
            connector.setMaxPostSize(30 * 1024 * 1024); // 30MB
        });
    }
}
