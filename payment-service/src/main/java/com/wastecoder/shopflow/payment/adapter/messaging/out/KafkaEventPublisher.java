package com.wastecoder.shopflow.payment.adapter.messaging.out;

import com.wastecoder.shopflow.payment.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.payment.application.port.out.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka implementation of {@link EventPublisher}. Wraps the payload in an {@link EventEnvelope},
 * stamping a fresh {@code eventId} and {@code occurredAt}, and sends it keyed by the order id so
 * that all messages for an order land on the same partition (per-order ordering).
 *
 * <p>When called inside a transaction (the reply emitted by a payment use case), the actual send is
 * deferred to {@code afterCommit}: the broker never sees a reply for state that later rolls back, and a
 * redelivery cannot observe an event whose dedup marker has not committed yet. Outside a transaction the
 * envelope is sent immediately. The envelope itself is built eagerly so {@code occurredAt} reflects when
 * the event happened, not when the commit flushed it.
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
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					kafkaTemplate.send(topic, key, envelope);
				}
			});
		} else {
			kafkaTemplate.send(topic, key, envelope);
		}
	}
}
