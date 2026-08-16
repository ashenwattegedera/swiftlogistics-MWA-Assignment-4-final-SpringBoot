package com.swiftlogistics.middleware.config;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * For the prototype demo and tests we run an in-process AMQP broker
 * ({@link MockConnectionFactory}) so no external RabbitMQ/Docker is required.
 *
 * <p>For a production-like deployment, start the real broker (see docker-compose.yml) and run
 * with {@code --spring.profiles.active=rabbitmq}: this configuration is then disabled and
 * Spring Boot's default connection factory (host/port from {@code spring.rabbitmq.*}) is used.</p>
 */
@Configuration
@Profile("!rabbitmq")
public class EmbeddedRabbitConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory(new MockConnectionFactory());
    }
}
