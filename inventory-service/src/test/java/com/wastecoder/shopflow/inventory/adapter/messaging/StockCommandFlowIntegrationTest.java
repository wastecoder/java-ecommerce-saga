package com.wastecoder.shopflow.inventory.adapter.messaging;

import com.wastecoder.shopflow.inventory.TestcontainersConfiguration;
import com.wastecoder.shopflow.inventory.application.port.out.EventPublisher;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.application.port.out.StockReservationRepository;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the real {@code inventory.commands} listener end-to-end, simulating order-service by publishing
 * commands through the app's own {@link EventPublisher}, and asserts both the reply on
 * {@code inventory.events} and the database side effects.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StockCommandFlowIntegrationTest {

	private static final String CAPTURE_ID = "flow-events-capture";
	private static final BlockingQueue<EventEnvelope> INVENTORY_EVENTS = new LinkedBlockingQueue<>();

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private StockReservationRepository reservationRepository;

	@Autowired
	private KafkaListenerEndpointRegistry registry;

	@BeforeEach
	void awaitAssignment() {
		// inventory.events is declared with 3 partitions; wait until the capture consumer owns them before
		// driving the saga so the reply cannot race the group's first rebalance.
		ContainerTestUtils.waitForAssignment(registry.getListenerContainer(CAPTURE_ID), 3);
	}

	@Test
	@DisplayName("Given enough stock, when a ReserveStock command arrives, then StockReserved is published and stock/reservation reflect the reservation")
	void reserve_happyPath() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		stockRepository.save(StockItemMother.aStockItemFor(productId, 100));

		sendCommand(MessageType.RESERVE_STOCK, orderId, productId, 2);

		assertThat(await(MessageType.STOCK_RESERVED, orderId)).isNotNull();
		awaitReservationStatus(orderId, ReservationStatus.RESERVED);
		StockItem stock = stockRepository.findByProductId(productId).orElseThrow();
		assertThat(stock.available()).isEqualTo(98);
		assertThat(stock.reserved()).isEqualTo(2);
		List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);
		assertThat(reservations).hasSize(1);
		assertThat(reservations.get(0).status()).isEqualTo(ReservationStatus.RESERVED);
	}

	@Test
	@DisplayName("Given insufficient stock, when a ReserveStock command arrives, then StockReservationFailed is published and stock is unchanged")
	void reserve_insufficient() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		stockRepository.save(StockItemMother.aStockItemFor(productId, 1));

		sendCommand(MessageType.RESERVE_STOCK, orderId, productId, 2);

		assertThat(await(MessageType.STOCK_RESERVATION_FAILED, orderId)).isNotNull();
		awaitReservationStatus(orderId, ReservationStatus.FAILED);
		StockItem stock = stockRepository.findByProductId(productId).orElseThrow();
		assertThat(stock.available()).isEqualTo(1);
		assertThat(stock.reserved()).isZero();
		assertThat(reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.FAILED)).hasSize(1);
	}

	@Test
	@DisplayName("Given a previously reserved order, when a ReleaseStock command arrives, then StockReleased is published and stock is restored")
	void release_restoresStock() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		stockRepository.save(StockItemMother.aStockItemFor(productId, 100));

		sendCommand(MessageType.RESERVE_STOCK, orderId, productId, 2);
		assertThat(await(MessageType.STOCK_RESERVED, orderId)).isNotNull();
		awaitReservationStatus(orderId, ReservationStatus.RESERVED);

		sendCommand(MessageType.RELEASE_STOCK, orderId, productId, 2);
		assertThat(await(MessageType.STOCK_RELEASED, orderId)).isNotNull();
		awaitReservationStatus(orderId, ReservationStatus.RELEASED);

		StockItem stock = stockRepository.findByProductId(productId).orElseThrow();
		assertThat(stock.available()).isEqualTo(100);
		assertThat(stock.reserved()).isZero();
		assertThat(reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RELEASED)).hasSize(1);
	}

	private void sendCommand(String type, UUID orderId, UUID productId, int quantity) {
		Map<String, Object> payload = Map.of(
				"orderId", orderId.toString(),
				"items", List.of(Map.of("productId", productId.toString(), "quantity", quantity)));
		eventPublisher.publish(Topics.INVENTORY_COMMANDS, orderId.toString(), type, payload);
	}

	private EventEnvelope await(String type, UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = INVENTORY_EVENTS.poll(1, TimeUnit.SECONDS);
			if (envelope != null && type.equals(envelope.type()) && orderId.toString().equals(envelope.orderId())) {
				return envelope;
			}
		}
		throw new AssertionError("Did not receive " + type + " for order " + orderId + " within timeout");
	}

	/**
	 * Polls until a reservation for the order reaches {@code status}. The reply is published after the use
	 * case's transaction commits, so observing it implies the rows are committed; the brief poll stays as a
	 * defensive guard against read-your-writes lag on the shared database.
	 */
	private void awaitReservationStatus(UUID orderId, ReservationStatus status) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			if (!reservationRepository.findByOrderIdAndStatus(orderId, status).isEmpty()) {
				return;
			}
			TimeUnit.MILLISECONDS.sleep(100);
		}
		throw new AssertionError("Reservation for order " + orderId + " did not reach " + status + " within timeout");
	}

	@TestConfiguration
	static class CaptureConfig {

		@KafkaListener(id = CAPTURE_ID, topics = Topics.INVENTORY_EVENTS, groupId = "inventory-flow-it-capture")
		void onInventoryEvent(EventEnvelope envelope) {
			INVENTORY_EVENTS.add(envelope);
		}
	}
}
