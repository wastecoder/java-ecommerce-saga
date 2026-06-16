package com.wastecoder.shopflow.order;

import com.wastecoder.shopflow.order.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.order.adapter.messaging.MessageType;
import com.wastecoder.shopflow.order.adapter.messaging.Topics;
import com.wastecoder.shopflow.order.application.port.in.PlaceOrderUseCase;
import com.wastecoder.shopflow.order.application.port.out.EventPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
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

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the order-service saga in isolation through its real listeners and publishers, simulating the
 * inventory and payment services by publishing fake reply events. The app's own EventPublisher is reused to
 * publish the fakes so they match the on-the-wire envelope exactly. The full cross-service flow, with all
 * services running against one broker, is covered by {@code OrderSagaEndToEndIntegrationTest} in the
 * integration-tests module.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderSagaIntegrationTest {

	private static final BlockingQueue<EventEnvelope> INVENTORY_COMMANDS = new LinkedBlockingQueue<>();
	private static final BlockingQueue<EventEnvelope> PAYMENT_COMMANDS = new LinkedBlockingQueue<>();
	private static final BlockingQueue<EventEnvelope> ORDER_EVENTS = new LinkedBlockingQueue<>();

	@Autowired
	private PlaceOrderUseCase placeOrderUseCase;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private OrderRepository orderRepository;

	@Test
	@DisplayName("Given a placed order, when stock is reserved and payment is authorized, then the order is confirmed")
	void confirmFlow() throws InterruptedException {
		UUID orderId = placeOrder();

		assertThat(await(INVENTORY_COMMANDS, MessageType.RESERVE_STOCK, orderId)).isNotNull();
		simulate(Topics.INVENTORY_EVENTS, MessageType.STOCK_RESERVED, orderId);

		assertThat(await(PAYMENT_COMMANDS, MessageType.PROCESS_PAYMENT, orderId)).isNotNull();
		simulate(Topics.PAYMENT_EVENTS, MessageType.PAYMENT_AUTHORIZED, orderId);

		assertThat(await(ORDER_EVENTS, MessageType.ORDER_CONFIRMED, orderId)).isNotNull();
		assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CONFIRMED);
	}

	@Test
	@DisplayName("Given a placed order, when stock reservation fails, then the order is rejected and no payment is requested")
	void rejectFlow() throws InterruptedException {
		UUID orderId = placeOrder();

		assertThat(await(INVENTORY_COMMANDS, MessageType.RESERVE_STOCK, orderId)).isNotNull();
		simulate(Topics.INVENTORY_EVENTS, MessageType.STOCK_RESERVATION_FAILED, orderId);

		assertThat(await(ORDER_EVENTS, MessageType.ORDER_REJECTED, orderId)).isNotNull();
		assertThat(statusOf(orderId)).isEqualTo(OrderStatus.REJECTED);
		assertNotReceived(PAYMENT_COMMANDS, MessageType.PROCESS_PAYMENT, orderId);
	}

	@Test
	@DisplayName("Given a stock-reserved order, when payment is refused, then the order is cancelled, stock is released (compensation) and the StockReleased ack is a no-op")
	void cancelAndCompensateFlow() throws InterruptedException {
		UUID orderId = placeOrder();

		assertThat(await(INVENTORY_COMMANDS, MessageType.RESERVE_STOCK, orderId)).isNotNull();
		simulate(Topics.INVENTORY_EVENTS, MessageType.STOCK_RESERVED, orderId);

		assertThat(await(PAYMENT_COMMANDS, MessageType.PROCESS_PAYMENT, orderId)).isNotNull();
		simulate(Topics.PAYMENT_EVENTS, MessageType.PAYMENT_FAILED, orderId);

		assertThat(await(INVENTORY_COMMANDS, MessageType.RELEASE_STOCK, orderId)).isNotNull();
		assertThat(await(ORDER_EVENTS, MessageType.ORDER_CANCELLED, orderId)).isNotNull();
		assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CANCELLED);

		// The compensation acknowledgement must not change the terminal order.
		simulate(Topics.INVENTORY_EVENTS, MessageType.STOCK_RELEASED, orderId);
		assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	@DisplayName("Given a stock-reserved order, when PaymentAuthorized is redelivered, then the order is confirmed only once")
	void redeliveryIsIdempotent() throws InterruptedException {
		UUID orderId = placeOrder();

		assertThat(await(INVENTORY_COMMANDS, MessageType.RESERVE_STOCK, orderId)).isNotNull();
		simulate(Topics.INVENTORY_EVENTS, MessageType.STOCK_RESERVED, orderId);
		assertThat(await(PAYMENT_COMMANDS, MessageType.PROCESS_PAYMENT, orderId)).isNotNull();

		simulate(Topics.PAYMENT_EVENTS, MessageType.PAYMENT_AUTHORIZED, orderId);
		simulate(Topics.PAYMENT_EVENTS, MessageType.PAYMENT_AUTHORIZED, orderId);

		assertThat(await(ORDER_EVENTS, MessageType.ORDER_CONFIRMED, orderId)).isNotNull();
		assertNotReceived(ORDER_EVENTS, MessageType.ORDER_CONFIRMED, orderId);
		assertThat(statusOf(orderId)).isEqualTo(OrderStatus.CONFIRMED);
	}

	private UUID placeOrder() {
		Order placed = placeOrderUseCase.execute(PlaceOrderCommandMother.aValidCommand());
		return placed.id();
	}

	private void simulate(String topic, String type, UUID orderId) {
		eventPublisher.publish(topic, orderId.toString(), type, Map.of("orderId", orderId.toString()));
	}

	private OrderStatus statusOf(UUID orderId) {
		return orderRepository.findById(orderId).orElseThrow().status();
	}

	private EventEnvelope await(BlockingQueue<EventEnvelope> queue, String type, UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = queue.poll(1, TimeUnit.SECONDS);
			if (matches(envelope, type, orderId)) {
				return envelope;
			}
		}
		throw new AssertionError("Did not receive " + type + " for order " + orderId + " within timeout");
	}

	private void assertNotReceived(BlockingQueue<EventEnvelope> queue, String type, UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(4).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = queue.poll(500, TimeUnit.MILLISECONDS);
			if (matches(envelope, type, orderId)) {
				throw new AssertionError("Unexpectedly received " + type + " for order " + orderId);
			}
		}
	}

	private boolean matches(EventEnvelope envelope, String type, UUID orderId) {
		return envelope != null && type.equals(envelope.type()) && orderId.toString().equals(envelope.orderId());
	}

	@TestConfiguration
	static class CaptureConfig {

		@KafkaListener(topics = Topics.INVENTORY_COMMANDS, groupId = "saga-it-capture")
		void onInventoryCommand(EventEnvelope envelope) {
			INVENTORY_COMMANDS.add(envelope);
		}

		@KafkaListener(topics = Topics.PAYMENT_COMMANDS, groupId = "saga-it-capture")
		void onPaymentCommand(EventEnvelope envelope) {
			PAYMENT_COMMANDS.add(envelope);
		}

		@KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "saga-it-capture")
		void onOrderEvent(EventEnvelope envelope) {
			ORDER_EVENTS.add(envelope);
		}
	}
}
