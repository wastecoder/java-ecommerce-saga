package com.wastecoder.shopflow.payment.adapter.messaging;

import com.wastecoder.shopflow.payment.TestcontainersConfiguration;
import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.application.port.out.ProcessedMessageRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the {@code payment.commands} consumer's resilience guarantees end-to-end against a real broker:
 * eventId idempotency (a redelivered command is handled once) and the dead letter topic (a malformed payload
 * or a non-deserializable poison-pill is routed to {@code payment.commands.DLT} rather than looping).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentCommandResilienceIntegrationTest {

	private static final BlockingQueue<EventEnvelope> PAYMENT_EVENTS = new LinkedBlockingQueue<>();
	private static final BlockingQueue<ConsumerRecord<String, byte[]>> DLT = new LinkedBlockingQueue<>();

	@Autowired
	private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	@Autowired
	private ProducerFactory<Object, Object> producerFactory;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ProcessedMessageRepository processedMessages;

	@Test
	@DisplayName("Given a ProcessPayment command redelivered with the same eventId, when consumed twice, then the payment is authorized once and only one reply is published")
	void redeliveredProcessPayment_authorizesOnce() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		EventEnvelope command = new EventEnvelope(eventId, MessageType.PROCESS_PAYMENT, orderId.toString(),
				Instant.now(), payload(orderId, new BigDecimal("100.00")));

		kafkaTemplate.send(Topics.PAYMENT_COMMANDS, orderId.toString(), command);
		kafkaTemplate.send(Topics.PAYMENT_COMMANDS, orderId.toString(), command);

		assertThat(awaitEvent(MessageType.PAYMENT_AUTHORIZED, orderId)).isNotNull();
		// The redelivery is short-circuited by the dedup before the use case, so it does NOT republish.
		assertNoMoreEvent(MessageType.PAYMENT_AUTHORIZED, orderId);
		assertThat(paymentRepository.findByOrderId(orderId)).isPresent();
		assertThat(processedMessages.existsByEventIdAndConsumer(eventId, Consumers.PAYMENT_COMMAND)).isTrue();
	}

	@Test
	@DisplayName("Given a ProcessPayment command with a malformed payload, when consumed, then it is routed to the DLT and is not marked processed")
	void malformedPayload_goesToDlt() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		Map<String, Object> badPayload = Map.of(
				"orderId", orderId.toString(),
				"customerId", UUID.randomUUID().toString(),
				"amount", "not-a-number");
		EventEnvelope command = new EventEnvelope(eventId, MessageType.PROCESS_PAYMENT, orderId.toString(),
				Instant.now(), badPayload);

		kafkaTemplate.send(Topics.PAYMENT_COMMANDS, orderId.toString(), command);

		assertThat(awaitDlt(orderId.toString())).isNotNull();
		assertThat(processedMessages.existsByEventIdAndConsumer(eventId, Consumers.PAYMENT_COMMAND)).isFalse();
	}

	@Test
	@DisplayName("Given a non-deserializable poison-pill record, when consumed, then it is routed to the DLT instead of looping the partition")
	void poisonPill_goesToDlt() throws InterruptedException {
		String key = "poison-" + UUID.randomUUID();

		sendRawBytes(Topics.PAYMENT_COMMANDS, key, "this is not valid envelope json".getBytes(StandardCharsets.UTF_8));

		assertThat(awaitDlt(key)).isNotNull();
	}

	private static Map<String, Object> payload(UUID orderId, BigDecimal amount) {
		return Map.of("orderId", orderId.toString(),
				"customerId", UUID.randomUUID().toString(),
				"amount", amount);
	}

	private EventEnvelope awaitEvent(String type, UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = PAYMENT_EVENTS.poll(1, TimeUnit.SECONDS);
			if (envelope != null && type.equals(envelope.type()) && orderId.toString().equals(envelope.orderId())) {
				return envelope;
			}
		}
		throw new AssertionError("Did not receive " + type + " for order " + orderId + " within timeout");
	}

	private void assertNoMoreEvent(String type, UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = PAYMENT_EVENTS.poll(500, TimeUnit.MILLISECONDS);
			if (envelope != null && type.equals(envelope.type()) && orderId.toString().equals(envelope.orderId())) {
				throw new AssertionError("Unexpected duplicate " + type + " for order " + orderId);
			}
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

		@KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "payment-resilience-events")
		void onPaymentEvent(EventEnvelope envelope) {
			PAYMENT_EVENTS.add(envelope);
		}

		@KafkaListener(topics = Topics.PAYMENT_COMMANDS + ".DLT", groupId = "payment-resilience-dlt",
				containerFactory = "dltBytesFactory")
		void onDeadLetter(ConsumerRecord<String, byte[]> dlqRecord) {
			DLT.add(dlqRecord);
		}
	}

	/**
	 * The byte[] DLT factory lives in its own config: declaring it on {@link CaptureConfig} (which carries
	 * the {@code @KafkaListener} methods) would create a circular reference, because resolving the listener's
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
			props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-resilience-dlt");
			props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			props.remove("spring.deserializer.key.delegate.class");
			props.remove("spring.deserializer.value.delegate.class");
			ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
			factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
			return factory;
		}
	}
}
