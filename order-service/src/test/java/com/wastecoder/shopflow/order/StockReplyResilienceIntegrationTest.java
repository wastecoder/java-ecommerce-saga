package com.wastecoder.shopflow.order;

import com.wastecoder.shopflow.order.adapter.messaging.Consumers;
import com.wastecoder.shopflow.order.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.order.adapter.messaging.MessageType;
import com.wastecoder.shopflow.order.adapter.messaging.Topics;
import com.wastecoder.shopflow.order.application.port.in.PlaceOrderUseCase;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.application.port.out.ProcessedMessageRepository;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderStatus;
import com.wastecoder.shopflow.order.testsupport.mother.PlaceOrderCommandMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the saga reply consumers' resilience guarantees end-to-end against a real broker: eventId
 * idempotency (a redelivered {@code StockReserved} advances the saga once) and the dead letter topic (a
 * reply with a malformed {@code orderId} is routed to {@code inventory.events.DLT} rather than looping).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StockReplyResilienceIntegrationTest {

	private static final BlockingQueue<EventEnvelope> PAYMENT_COMMANDS = new LinkedBlockingQueue<>();
	private static final BlockingQueue<EventEnvelope> DLT = new LinkedBlockingQueue<>();

	@Autowired
	private PlaceOrderUseCase placeOrderUseCase;

	@Autowired
	private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProcessedMessageRepository processedMessages;

	@Test
	@DisplayName("Given a StockReserved reply redelivered with the same eventId, when consumed twice, then the saga advances once and only one ProcessPayment is requested")
	void redeliveredStockReserved_advancesOnce() throws InterruptedException {
		UUID orderId = placeOrder();
		UUID eventId = UUID.randomUUID();
		EventEnvelope reply = new EventEnvelope(eventId, MessageType.STOCK_RESERVED, orderId.toString(),
				Instant.now(), Map.of("orderId", orderId.toString()));

		kafkaTemplate.send(Topics.INVENTORY_EVENTS, orderId.toString(), reply);
		kafkaTemplate.send(Topics.INVENTORY_EVENTS, orderId.toString(), reply);

		assertThat(await(PAYMENT_COMMANDS, MessageType.PROCESS_PAYMENT, orderId.toString())).isNotNull();
		assertNotReceived(PAYMENT_COMMANDS, MessageType.PROCESS_PAYMENT, orderId.toString());
		awaitStatus(orderId, OrderStatus.STOCK_RESERVED);
		assertThat(processedMessages.existsByEventIdAndConsumer(eventId, Consumers.STOCK_REPLY)).isTrue();
	}

	@Test
	@DisplayName("Given a stock reply with a malformed orderId, when consumed, then it is routed to the DLT and is not marked processed")
	void malformedOrderId_goesToDlt() throws InterruptedException {
		UUID eventId = UUID.randomUUID();
		EventEnvelope reply = new EventEnvelope(eventId, MessageType.STOCK_RESERVED, "not-a-uuid", Instant.now(),
				Map.of());

		kafkaTemplate.send(Topics.INVENTORY_EVENTS, "not-a-uuid", reply);

		EventEnvelope dead = await(DLT, MessageType.STOCK_RESERVED, "not-a-uuid");
		assertThat(dead).isNotNull();
		assertThat(dead.eventId()).isEqualTo(eventId);
		assertThat(processedMessages.existsByEventIdAndConsumer(eventId, Consumers.STOCK_REPLY)).isFalse();
	}

	private UUID placeOrder() {
		Order placed = placeOrderUseCase.execute(PlaceOrderCommandMother.aValidCommand());
		return placed.id();
	}

	/**
	 * Polls until the order reaches {@code status} — the order event can be observed before the coordinator's
	 * transaction commits, so poll the source of truth instead of reading once.
	 */
	private void awaitStatus(UUID orderId, OrderStatus status) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			Order order = orderRepository.findById(orderId).orElse(null);
			if (order != null && order.status() == status) {
				return;
			}
			TimeUnit.MILLISECONDS.sleep(100);
		}
		throw new AssertionError("Order " + orderId + " did not reach " + status + " within timeout");
	}

	private EventEnvelope await(BlockingQueue<EventEnvelope> queue, String type, String orderId)
			throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = queue.poll(1, TimeUnit.SECONDS);
			if (envelope != null && type.equals(envelope.type()) && orderId.equals(envelope.orderId())) {
				return envelope;
			}
		}
		throw new AssertionError("Did not receive " + type + " for order " + orderId + " within timeout");
	}

	private void assertNotReceived(BlockingQueue<EventEnvelope> queue, String type, String orderId)
			throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = queue.poll(500, TimeUnit.MILLISECONDS);
			if (envelope != null && type.equals(envelope.type()) && orderId.equals(envelope.orderId())) {
				throw new AssertionError("Unexpectedly received a duplicate " + type + " for order " + orderId);
			}
		}
	}

	@TestConfiguration
	static class CaptureConfig {

		@KafkaListener(topics = Topics.PAYMENT_COMMANDS, groupId = "order-resilience-payment")
		void onPaymentCommand(EventEnvelope envelope) {
			PAYMENT_COMMANDS.add(envelope);
		}

		@KafkaListener(topics = Topics.INVENTORY_EVENTS + ".DLT", groupId = "order-resilience-dlt")
		void onDeadLetter(EventEnvelope envelope) {
			DLT.add(envelope);
		}
	}
}
