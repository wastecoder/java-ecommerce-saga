package com.wastecoder.shopflow.notification.adapter.messaging;

import com.wastecoder.shopflow.notification.TestcontainersConfiguration;
import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.application.port.out.ProcessedMessageRepository;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.testsupport.mother.OrderEventMother;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the {@code order.events} consumer's resilience guarantees end-to-end against a real broker:
 * eventId idempotency (a redelivered event records the notification once) and the dead letter topic (a
 * malformed payload or a non-deserializable poison-pill is routed to {@code order.events.DLT} rather than
 * looping the partition).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationResilienceIntegrationTest {

	private static final BlockingQueue<ConsumerRecord<String, byte[]>> DLT = new LinkedBlockingQueue<>();

	@Autowired
	private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	@Autowired
	private ProducerFactory<Object, Object> producerFactory;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ProcessedMessageRepository processedMessages;

	@Test
	@DisplayName("Given an order event redelivered with the same eventId, when consumed twice, then the notification is recorded once")
	void redeliveredEvent_recordsOnce() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		EventEnvelope event = new EventEnvelope(eventId, MessageType.ORDER_CONFIRMED, orderId.toString(), Instant.now(),
				OrderEventMother.aPayloadMap(orderId, UUID.randomUUID(), new BigDecimal("100.00"), "CONFIRMED"));

		kafkaTemplate.send(Topics.ORDER_EVENTS, orderId.toString(), event);
		kafkaTemplate.send(Topics.ORDER_EVENTS, orderId.toString(), event);

		awaitAtLeastOne(orderId);
		// The redelivery is short-circuited by the dedup before the use case, so it does NOT record again.
		assertStaysAtOne(orderId);
		assertThat(processedMessages.existsByEventIdAndConsumer(eventId, Consumers.ORDER_EVENT)).isTrue();
	}

	@Test
	@DisplayName("Given an order event with a malformed payload, when consumed, then it is routed to the DLT and is not marked processed")
	void malformedPayload_goesToDlt() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		Map<String, Object> badPayload = Map.of(
				"orderId", orderId.toString(),
				"customerId", UUID.randomUUID().toString(),
				"totalAmount", "not-a-number",
				"status", "CONFIRMED");
		EventEnvelope event = new EventEnvelope(eventId, MessageType.ORDER_CONFIRMED, orderId.toString(), Instant.now(),
				badPayload);

		kafkaTemplate.send(Topics.ORDER_EVENTS, orderId.toString(), event);

		assertThat(awaitDlt(orderId.toString())).isNotNull();
		assertThat(processedMessages.existsByEventIdAndConsumer(eventId, Consumers.ORDER_EVENT)).isFalse();
	}

	@Test
	@DisplayName("Given a non-deserializable poison-pill record, when consumed, then it is routed to the DLT instead of looping the partition")
	void poisonPill_goesToDlt() throws InterruptedException {
		String key = "poison-" + UUID.randomUUID();

		sendRawBytes(Topics.ORDER_EVENTS, key, "this is not valid envelope json".getBytes(StandardCharsets.UTF_8));

		assertThat(awaitDlt(key)).isNotNull();
	}

	private void awaitAtLeastOne(UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			if (!notificationRepository.findByOrderId(orderId).isEmpty()) {
				return;
			}
			Thread.sleep(200);
		}
		throw new AssertionError("No notification recorded for order " + orderId + " within timeout");
	}

	private void assertStaysAtOne(UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (System.nanoTime() < deadline) {
			List<Notification> recorded = notificationRepository.findByOrderId(orderId);
			assertThat(recorded).hasSize(1);
			Thread.sleep(500);
		}
	}

	private ConsumerRecord<String, byte[]> awaitDlt(String key) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			ConsumerRecord<String, byte[]> dlqRecord = DLT.poll(1, TimeUnit.SECONDS);
			if (dlqRecord != null && key.equals(dlqRecord.key())) {
				return dlqRecord;
			}
		}
		throw new AssertionError("Did not receive a DLT record for key " + key + " within timeout");
	}

	private void sendRawBytes(String topic, String key, byte[] value) {
		Map<String, Object> props = new HashMap<>(producerFactory.getConfigurationProperties());
		try (Producer<String, byte[]> producer = new KafkaProducer<>(props, new StringSerializer(),
				new ByteArraySerializer())) {
			producer.send(new ProducerRecord<>(topic, key, value));
			producer.flush();
		}
	}

	@TestConfiguration
	static class CaptureConfig {

		@KafkaListener(topics = Topics.ORDER_EVENTS + ".DLT", groupId = "notification-resilience-dlt",
				containerFactory = "dltBytesFactory")
		void onDeadLetter(ConsumerRecord<String, byte[]> dlqRecord) {
			DLT.add(dlqRecord);
		}
	}

	/**
	 * The byte[] DLT factory lives in its own config: declaring it on {@link CaptureConfig} (which carries
	 * the {@code @KafkaListener} method) would create a circular reference, because resolving the listener's
	 * {@code containerFactory} runs while that same config bean is still being created.
	 */
	@TestConfiguration
	static class DltBytesFactoryConfig {

		/** Reads the DLT as raw bytes so a non-deserializable poison-pill is captured rather than re-failing. */
		@Bean
		ConcurrentKafkaListenerContainerFactory<String, byte[]> dltBytesFactory(ConsumerFactory<Object, Object> base) {
			Map<String, Object> props = new HashMap<>(base.getConfigurationProperties());
			props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
			props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
			props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-resilience-dlt");
			props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			props.remove("spring.deserializer.key.delegate.class");
			props.remove("spring.deserializer.value.delegate.class");
			ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
			factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
			return factory;
		}
	}
}
