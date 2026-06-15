package com.wastecoder.shopflow.payment.adapter.messaging.out;

import com.wastecoder.shopflow.payment.adapter.messaging.EventEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

	@Mock
	private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	@InjectMocks
	private KafkaEventPublisher publisher;

	@Test
	@DisplayName("Given a payload, when publishing, then it sends an envelope to the topic keyed by orderId with a generated id and timestamp")
	void publish_sendsEnvelopeKeyedByOrderId() {
		String topic = "payment.events";
		String orderId = UUID.randomUUID().toString();
		Map<String, Object> payload = Map.of("orderId", orderId);

		publisher.publish(topic, orderId, "PaymentAuthorized", payload);

		ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
		verify(kafkaTemplate).send(eq(topic), eq(orderId), captor.capture());
		EventEnvelope sent = captor.getValue();
		assertThat(sent.type()).isEqualTo("PaymentAuthorized");
		assertThat(sent.orderId()).isEqualTo(orderId);
		assertThat(sent.payload()).isEqualTo(payload);
		assertThat(sent.eventId()).isNotNull();
		assertThat(sent.occurredAt()).isNotNull();
	}
}
