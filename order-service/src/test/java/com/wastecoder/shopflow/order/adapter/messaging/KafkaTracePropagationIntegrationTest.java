package com.wastecoder.shopflow.order.adapter.messaging;

import com.wastecoder.shopflow.order.TestcontainersConfiguration;
import com.wastecoder.shopflow.order.application.port.out.EventPublisher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.utils.ContainerTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the tracing requirement of Fase 5 item 2: with observation enabled on the KafkaTemplate
 * (producer) and the listener container, the trace context is propagated <em>through the Kafka
 * message</em> — the producer injects a W3C {@code traceparent} header that the consumer receives.
 * This is the per-message building block that lets a single saga show up as one end-to-end trace in
 * Zipkin. Span export to Zipkin is disabled in tests (no backend); only the propagated header is asserted.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class KafkaTracePropagationIntegrationTest {

	private static final String CAPTURE_ID = "trace-events-capture";
	private static final BlockingQueue<ConsumerRecord<String, EventEnvelope>> RECEIVED = new LinkedBlockingQueue<>();

	@Autowired
	private EventPublisher publisher;

	@Autowired
	private KafkaListenerEndpointRegistry registry;

	@BeforeEach
	void awaitAssignment() {
		// order.events is declared with 3 partitions; wait until the capture consumer owns them so the
		// publish below cannot race the group's first rebalance.
		ContainerTestUtils.waitForAssignment(registry.getListenerContainer(CAPTURE_ID), 3);
	}

	@Test
	@DisplayName("Given Kafka observation is enabled, when an envelope is published, then the consumed record carries a W3C traceparent header (trace context propagated over Kafka)")
	void publish_propagatesTraceContextHeader() throws InterruptedException {
		RECEIVED.clear();
		String orderId = UUID.randomUUID().toString();
		Map<String, Object> payload = Map.of("productId", "SKU-1", "quantity", 1);

		publisher.publish("order.events", orderId, "OrderCreated", payload);

		// The broker is shared across the suite, so order.events also carries other tests' events;
		// match on this order's key rather than asserting on the first record polled.
		ConsumerRecord<String, EventEnvelope> record = awaitRecordFor(orderId);
		assertThat(record).as("a record for this order should be consumed from order.events").isNotNull();

		Header traceparent = record.headers().lastHeader("traceparent");
		assertThat(traceparent).as("the producer should inject the W3C trace context into the Kafka headers").isNotNull();
		String value = new String(traceparent.value(), StandardCharsets.UTF_8);
		assertThat(value)
			.as("traceparent must follow the W3C format 00-<32 hex trace-id>-<16 hex span-id>-<2 hex flags>")
			.matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");
	}

	private ConsumerRecord<String, EventEnvelope> awaitRecordFor(String orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			ConsumerRecord<String, EventEnvelope> record = RECEIVED.poll(1, TimeUnit.SECONDS);
			if (record != null && orderId.equals(record.key())) {
				return record;
			}
		}
		return null;
	}

	@TestConfiguration
	static class TestListenerConfig {

		@KafkaListener(id = CAPTURE_ID, topics = "order.events", groupId = "order-trace-propagation-test")
		void collect(ConsumerRecord<String, EventEnvelope> record) {
			RECEIVED.add(record);
		}
	}
}
