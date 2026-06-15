package com.wastecoder.shopflow.notification.adapter.config;

import com.wastecoder.shopflow.notification.adapter.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics that notification-service owns. As a consumer of {@code order.events} it owns its
 * own dead letter, so it provisions {@code order.events.DLT} with the same partition count as the source (the
 * recoverer in {@link KafkaErrorHandlingConfig} routes a failed record to the same partition number).
 *
 * <p>{@code order.events} itself is owned (declared) by order-service, the producer, and is not declared
 * here. Spring Boot's auto-configured {@code KafkaAdmin} creates the {@link NewTopic} bean on startup.
 */
@Configuration
public class KafkaTopicsConfig {

	static final int PARTITIONS = 3;
	static final short REPLICAS = 1;

	@Bean
	NewTopic orderEventsDltTopic() {
		return TopicBuilder.name(Topics.ORDER_EVENTS + ".DLT").partitions(PARTITIONS).replicas(REPLICAS).build();
	}
}
