package com.swiftlogistics.middleware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated executor for saga processing so the broker consumer thread is never blocked by a
 * slow CMS/WMS/ROS call — the middleware stays responsive under high order volume.
 */
@Configuration
public class SagaConfig {

    @Bean(name = "sagaTaskExecutor")
    public TaskExecutor sagaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("saga-");
        executor.initialize();
        return executor;
    }
}
