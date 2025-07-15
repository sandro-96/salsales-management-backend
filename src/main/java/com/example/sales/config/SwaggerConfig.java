// File: com/example/sales/config/SwaggerConfig.java
package com.example.sales.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sales Management API")
                        .version("1.0")
                        .description("API cho hệ thống quản lý bán hàng"));
    }
}