package com.wastecoder.shopflow.inventory.adapter.messaging.out;

import com.wastecoder.shopflow.inventory.adapter.messaging.EventEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

	@Mock
	private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	@InjectMocks
	private KafkaEventPublisher publisher;

	@Test
	@DisplayName("Given no active transaction, when publishing, then it sends an envelope to the topic keyed by orderId with a generated id and timestamp")
	void publish_noTransaction_sendsEnvelopeKeyedByOrderId() {
		String topic = "inventory.events";
		String orderId = UUID.randomUUID().toString();
		Map<String, Object> payload = Map.of("productId", "SKU-1", "quantity", 2);

		publisher.publish(topic, orderId, "StockReserved", payload);

		ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
		verify(kafkaTemplate).send(eq(topic), eq(orderId), captor.capture());
		EventEnvelope sent = captor.getValue();
		assertThat(sent.type()).isEqualTo("StockReserved");
		assertThat(sent.orderId()).isEqualTo(orderId);
		assertThat(sent.payload()).isEqualTo(payload);
		assertThat(sent.eventId()).isNotNull();
		assertThat(sent.occurredAt()).isNotNull();
	}

	@Test
	@DisplayName("Given an active transaction, when publishing, then the send is deferred until the transaction commits (no event for state that may roll back)")
	void publish_withinTransaction_defersSendUntilAfterCommit() {
		String topic = "inventory.events";
		String orderId = UUID.randomUUID().toString();

		TransactionSynchronizationManager.initSynchronization();
		try {
			publisher.publish(topic, orderId, "StockReserved", Map.of("productId", "SKU-1", "quantity", 2));

			// Still inside the transaction: nothing must have been sent yet.
			verify(kafkaTemplate, never()).send(any(), any(), any());

			// Simulate the commit: drive the registered synchronizations' afterCommit callback.
			for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
				synchronization.afterCommit();
			}

			verify(kafkaTemplate).send(eq(topic), eq(orderId), any(EventEnvelope.class));
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}
}
