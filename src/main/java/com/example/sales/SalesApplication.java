// File: src/main/java/com/example/sales/SalesApplication.java
package com.example.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableMongoAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
@SpringBootApplication
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SalesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalesApplication.class, args);
	}

}
