package com.wastecoder.shopflow.inventory.adapter.messaging;

import com.wastecoder.shopflow.inventory.TestcontainersConfiguration;
import com.wastecoder.shopflow.inventory.application.port.out.ProcessedMessageRepository;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.application.port.out.StockReservationRepository;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
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
 * Exercises the {@code inventory.commands} consumer's resilience guarantees end-to-end against a real broker:
 * eventId idempotency (a redelivered command is handled once) and the dead letter topic (a malformed payload
 * or a non-deserializable poison-pill is routed to {@code inventory.commands.DLT} rather than looping).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StockCommandResilienceIntegrationTest {

	private static final BlockingQueue<EventEnvelope> INVENTORY_EVENTS = new LinkedBlockingQueue<>();
	private static final BlockingQueue<ConsumerRecord<String, byte[]>> DLT = new LinkedBlockingQueue<>();

	@Autowired
	private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	@Autowired
	private ProducerFactory<Object, Object> producerFactory;

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private StockReservationRepository reservationRepository;

	@Autowired
	private ProcessedMessageRepository processedMessages;

	@Test
	@DisplayName("Given a ReserveStock command redelivered with the same eventId, when consumed twice, then stock is reserved once and only one reply is published")
	void redeliveredReserveStock_reservesOnce() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		stockRepository.save(StockItemMother.aStockItemFor(productId, 100));
		EventEnvelope command = new EventEnvelope(eventId, MessageType.RESERVE_STOCK, orderId.toString(),
				Instant.now(), payload(orderId, productId, 2));

		kafkaTemplate.send(Topics.INVENTORY_COMMANDS, orderId.toString(), command);
		kafkaTemplate.send(Topics.INVENTORY_COMMANDS, orderId.toString(), command);

		assertThat(awaitEvent(MessageType.STOCK_RESERVED, orderId)).isNotNull();
		// The redelivery is short-circuited by the dedup before the use case, so it does NOT republish.
		assertNoMoreEvent(MessageType.STOCK_RESERVED, orderId);
		StockItem stock = stockRepository.findByProductId(productId).orElseThrow();
		assertThat(stock.available()).isEqualTo(98);
		assertThat(stock.reserved()).isEqualTo(2);
		assertThat(reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED)).hasSize(1);
		assertThat(processedMessages.existsByEventIdAndConsumer(eventId, Consumers.STOCK_COMMAND)).isTrue();
	}

	@Test
	@DisplayName("Given a ReserveStock command with a malformed payload, when consumed, then it is routed to the DLT and is not marked processed")
	void malformedPayload_goesToDlt() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		Map<String, Object> badPayload = Map.of(
				"orderId", orderId.toString(),
				"items", List.of(Map.of("productId", UUID.randomUUID().toString(), "quantity", "not-a-number")));
		EventEnvelope command = new EventEnvelope(eventId, MessageType.RESERVE_STOCK, orderId.toString(),
				Instant.now(), badPayload);

		kafkaTemplate.send(Topics.INVENTORY_COMMANDS, orderId.toString(), command);

		assertThat(awaitDlt(orderId.toString())).isNotNull();
		assertThat(processedMessages.existsByEventIdAndConsumer(eventId, Consumers.STOCK_COMMAND)).isFalse();
	}

	@Test
	@DisplayName("Given a non-deserializable poison-pill record, when consumed, then it is routed to the DLT instead of looping the partition")
	void poisonPill_goesToDlt() throws InterruptedException {
		String key = "poison-" + UUID.randomUUID();

		sendRawBytes(Topics.INVENTORY_COMMANDS, key, "this is not valid envelope json".getBytes(StandardCharsets.UTF_8));

		assertThat(awaitDlt(key)).isNotNull();
	}

	private static Map<String, Object> payload(UUID orderId, UUID productId, int quantity) {
		return Map.of("orderId", orderId.toString(),
				"items", List.of(Map.of("productId", productId.toString(), "quantity", quantity)));
	}

	private EventEnvelope awaitEvent(String type, UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = INVENTORY_EVENTS.poll(1, TimeUnit.SECONDS);
			if (envelope != null && type.equals(envelope.type()) && orderId.toString().equals(envelope.orderId())) {
				return envelope;
			}
		}
		throw new AssertionError("Did not receive " + type + " for order " + orderId + " within timeout");
	}

	private void assertNoMoreEvent(String type, UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = INVENTORY_EVENTS.poll(500, TimeUnit.MILLISECONDS);
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

		@KafkaListener(topics = Topics.INVENTORY_EVENTS, groupId = "inventory-resilience-events")
		void onInventoryEvent(EventEnvelope envelope) {
			INVENTORY_EVENTS.add(envelope);
		}

		@KafkaListener(topics = Topics.INVENTORY_COMMANDS + ".DLT", groupId = "inventory-resilience-dlt",
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
			props.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-resilience-dlt");
			props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			props.remove("spring.deserializer.key.delegate.class");
			props.remove("spring.deserializer.value.delegate.class");
			ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
			factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
			return factory;
		}
	}
}
