package com.wastecoder.shopflow.order.adapter.messaging;

import com.wastecoder.shopflow.order.TestcontainersConfiguration;
import com.wastecoder.shopflow.order.application.port.out.EventPublisher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class KafkaEnvelopeRoundTripIntegrationTest {

	private static final BlockingQueue<ConsumerRecord<String, EventEnvelope>> RECEIVED = new LinkedBlockingQueue<>();

	@Autowired
	private EventPublisher publisher;

	@Test
	@DisplayName("Given an envelope published to order.events, when it is consumed, then the key, fields and payload round-trip with no Spring type headers")
	void publishAndConsume_roundTrips() throws InterruptedException {
		RECEIVED.clear();
		String orderId = UUID.randomUUID().toString();
		Map<String, Object> payload = Map.of("productId", "SKU-1", "quantity", 2);

		publisher.publish("order.events", orderId, "OrderCreated", payload);

		ConsumerRecord<String, EventEnvelope> record = RECEIVED.poll(15, TimeUnit.SECONDS);
		assertThat(record).as("a record should be consumed from order.events").isNotNull();
		assertThat(record.key()).isEqualTo(orderId);
		assertThat(record.headers().lastHeader("__TypeId__")).as("no Spring type-info header is written").isNull();

		EventEnvelope envelope = record.value();
		assertThat(envelope.type()).isEqualTo("OrderCreated");
		assertThat(envelope.orderId()).isEqualTo(orderId);
		assertThat(envelope.eventId()).isNotNull();
		assertThat(envelope.occurredAt()).isNotNull();
		assertThat(envelope.payload()).isInstanceOf(Map.class);
		Map<?, ?> payloadMap = (Map<?, ?>) envelope.payload();
		assertThat(payloadMap.get("productId")).isEqualTo("SKU-1");
		assertThat(((Number) payloadMap.get("quantity")).intValue()).isEqualTo(2);
	}

	@TestConfiguration
	static class TestListenerConfig {

		@KafkaListener(topics = "order.events", groupId = "order-roundtrip-test")
		void collect(ConsumerRecord<String, EventEnvelope> record) {
			RECEIVED.add(record);
		}
	}
}
