package com.example.sales.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Số thread cơ bản
        executor.setMaxPoolSize(20); // Số thread tối đa
        executor.setQueueCapacity(100); // Hàng đợi tác vụ
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
}