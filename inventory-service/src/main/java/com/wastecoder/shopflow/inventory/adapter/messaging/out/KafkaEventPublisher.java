package com.wastecoder.shopflow.inventory.adapter.messaging.out;

import com.wastecoder.shopflow.inventory.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.inventory.application.port.out.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka implementation of {@link EventPublisher}. Wraps the payload in an {@link EventEnvelope},
 * stamping a fresh {@code eventId} and {@code occurredAt}, and sends it keyed by the order id so
 * that all messages for an order land on the same partition (per-order ordering).
 *
 * <p>The {@link KafkaTemplate} is auto-configured by Spring Boot from {@code spring.kafka.*}; the
 * idempotent producer and JSON serialization are configured there.
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

	private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	public KafkaEventPublisher(KafkaTemplate<String, EventEnvelope> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void publish(String topic, String key, String type, Object payload) {
		EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), type, key, Instant.now(), payload);
		kafkaTemplate.send(topic, key, envelope);
	}
}
