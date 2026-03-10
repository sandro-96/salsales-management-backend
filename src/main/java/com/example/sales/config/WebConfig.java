// File: src/main/java/com/example/sales/config/WebConfig.java
package com.example.sales.config;

import com.example.sales.security.PlanInterceptor;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.unit.DataSize;
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
                .addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/app-uploads/")
                .addResourceLocations("file:/var/www/app-uploads/");
    }

    /**
     * Cấu hình multipart:
     * - maxFileSize   : 5MB/file
     * - maxRequestSize: 30MB/request (10 ảnh × 3MB)
     *
     * Quan trọng: Spring Boot đọc multipart config từ bean này khi
     * spring.servlet.multipart.enabled=true (mặc định).
     * Dùng MultipartConfigFactory để set maxParts = 20
     * (10 ảnh + 1 JSON part + buffer dư).
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(5));
        factory.setMaxRequestSize(DataSize.ofMegabytes(30));
        return factory.createMultipartConfig();
    }

    /**
     * Tăng maxParameterCount của Tomcat connector để xử lý được 11+ parts
     * (1 JSON part "product" + tối đa 10 file ảnh).
     * Mặc định Tomcat 10 giới hạn 10.000 parameters nhưng FileCount
     * được set riêng trong FileUpload library — cần override qua
     * addContextCustomizers với allowCasualMultipartParsing.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                connector.setMaxPostSize(30 * 1024 * 1024); // 30MB
                connector.setMaxParameterCount(200);        // dư cho mọi params + parts
            });
            factory.addContextCustomizers(context ->
                    context.setAllowCasualMultipartParsing(true)
            );
        };
    }
}
