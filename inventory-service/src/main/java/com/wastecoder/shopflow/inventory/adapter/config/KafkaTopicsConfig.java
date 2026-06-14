package com.wastecoder.shopflow.inventory.adapter.config;

import com.wastecoder.shopflow.inventory.adapter.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics that inventory-service owns (producer-owns-topic). Spring Boot's
 * auto-configured {@code KafkaAdmin} creates these {@link NewTopic} beans on startup.
 *
 * <p>Single-node KRaft dev broker: replication factor 1. Three partitions allow per-order
 * parallelism while {@code key=orderId} keeps each order's messages on one partition (ordering).
 *
 * <p>The {@code *.DLT} topic is declared by the consuming service (it owns its own dead letter): this
 * service consumes {@code inventory.commands}, so it provisions {@code inventory.commands.DLT} with the
 * same partition count as the source (the recoverer routes a failed record to the same partition number).
 */
@Configuration
public class KafkaTopicsConfig {

	static final int PARTITIONS = 3;
	static final short REPLICAS = 1;

	@Bean
	NewTopic inventoryEventsTopic() {
		return TopicBuilder.name(Topics.INVENTORY_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
	}

	@Bean
	NewTopic inventoryCommandsDltTopic() {
		return TopicBuilder.name(Topics.INVENTORY_COMMANDS + ".DLT").partitions(PARTITIONS).replicas(REPLICAS).build();
	}
}
