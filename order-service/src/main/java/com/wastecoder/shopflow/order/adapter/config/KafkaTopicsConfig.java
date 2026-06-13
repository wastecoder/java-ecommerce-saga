package com.wastecoder.shopflow.order.adapter.config;

import com.wastecoder.shopflow.order.adapter.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics that order-service owns (producer-owns-topic). Spring Boot's
 * auto-configured {@code KafkaAdmin} creates these {@link NewTopic} beans on startup.
 *
 * <p>Single-node KRaft dev broker: replication factor 1. Three partitions allow per-order
 * parallelism while {@code key=orderId} keeps each order's messages on one partition (ordering).
 * Dead-letter topics ({@code *.DLT}) are introduced with the consumer error handling (Fase 2, item 4).
 */
@Configuration
public class KafkaTopicsConfig {

	static final int PARTITIONS = 3;
	static final short REPLICAS = 1;

	@Bean
	NewTopic inventoryCommandsTopic() {
		return TopicBuilder.name(Topics.INVENTORY_COMMANDS).partitions(PARTITIONS).replicas(REPLICAS).build();
	}

	@Bean
	NewTopic paymentCommandsTopic() {
		return TopicBuilder.name(Topics.PAYMENT_COMMANDS).partitions(PARTITIONS).replicas(REPLICAS).build();
	}

	@Bean
	NewTopic orderEventsTopic() {
		return TopicBuilder.name(Topics.ORDER_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
	}
}
