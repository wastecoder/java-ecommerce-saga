package com.wastecoder.shopflow.payment.adapter.messaging;

import com.wastecoder.shopflow.payment.TestcontainersConfiguration;
import com.wastecoder.shopflow.payment.application.port.out.EventPublisher;
import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the real {@code payment.commands} listener end-to-end, simulating order-service by publishing
 * commands through the app's own {@link EventPublisher}, and asserts both the reply on
 * {@code payment.events} and the persisted payment. With the default PSP mode (DECLINE_ABOVE_THRESHOLD,
 * threshold 1000.00) the command amount steers the outcome, so a single running app covers approve, decline
 * and refund.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentCommandFlowIntegrationTest {

	private static final String CAPTURE_ID = "flow-events-capture";
	private static final BlockingQueue<EventEnvelope> PAYMENT_EVENTS = new LinkedBlockingQueue<>();

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private KafkaListenerEndpointRegistry registry;

	@BeforeEach
	void awaitAssignment() {
		// payment.events is declared with 3 partitions; wait until the capture consumer owns them before
		// driving the saga so the reply cannot race the group's first rebalance.
		ContainerTestUtils.waitForAssignment(registry.getListenerContainer(CAPTURE_ID), 3);
	}

	@Test
	@DisplayName("Given an amount below the decline threshold, when a ProcessPayment command arrives, then PaymentAuthorized is published and an AUTHORIZED payment is persisted")
	void process_approved() throws InterruptedException {
		UUID orderId = UUID.randomUUID();

		sendProcess(orderId, new BigDecimal("100.00"));

		assertThat(await(MessageType.PAYMENT_AUTHORIZED, orderId)).isNotNull();
		Payment payment = awaitPayment(orderId, PaymentStatus.AUTHORIZED);
		assertThat(payment.amount()).isEqualByComparingTo("100.00");
		assertThat(payment.providerRef()).startsWith("PSP-");
	}

	@Test
	@DisplayName("Given an amount at or above the decline threshold, when a ProcessPayment command arrives, then PaymentFailed is published and a FAILED payment is persisted")
	void process_declined() throws InterruptedException {
		UUID orderId = UUID.randomUUID();

		sendProcess(orderId, new BigDecimal("5000.00"));

		assertThat(await(MessageType.PAYMENT_FAILED, orderId)).isNotNull();
		awaitPayment(orderId, PaymentStatus.FAILED);
	}

	@Test
	@DisplayName("Given an authorized payment, when a RefundPayment command arrives, then PaymentRefunded is published and the payment becomes REFUNDED")
	void refund_refundsAuthorizedPayment() throws InterruptedException {
		UUID orderId = UUID.randomUUID();

		sendProcess(orderId, new BigDecimal("100.00"));
		assertThat(await(MessageType.PAYMENT_AUTHORIZED, orderId)).isNotNull();
		awaitPayment(orderId, PaymentStatus.AUTHORIZED);

		sendRefund(orderId);
		assertThat(await(MessageType.PAYMENT_REFUNDED, orderId)).isNotNull();
		awaitPayment(orderId, PaymentStatus.REFUNDED);
	}

	private void sendProcess(UUID orderId, BigDecimal amount) {
		Map<String, Object> payload = Map.of(
				"orderId", orderId.toString(),
				"customerId", UUID.randomUUID().toString(),
				"amount", amount);
		eventPublisher.publish(Topics.PAYMENT_COMMANDS, orderId.toString(), MessageType.PROCESS_PAYMENT, payload);
	}

	private void sendRefund(UUID orderId) {
		Map<String, Object> payload = Map.of("orderId", orderId.toString());
		eventPublisher.publish(Topics.PAYMENT_COMMANDS, orderId.toString(), MessageType.REFUND_PAYMENT, payload);
	}

	private EventEnvelope await(String type, UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			EventEnvelope envelope = PAYMENT_EVENTS.poll(1, TimeUnit.SECONDS);
			if (envelope != null && type.equals(envelope.type()) && orderId.toString().equals(envelope.orderId())) {
				return envelope;
			}
		}
		throw new AssertionError("Did not receive " + type + " for order " + orderId + " within timeout");
	}

	/**
	 * Polls the repository until the payment reaches {@code status}. The reply is published after the use
	 * case's transaction commits, so observing it implies the row is committed; the brief poll stays as a
	 * defensive guard against read-your-writes lag on the shared database.
	 */
	private Payment awaitPayment(UUID orderId, PaymentStatus status) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
			if (payment != null && payment.status() == status) {
				return payment;
			}
			TimeUnit.MILLISECONDS.sleep(100);
		}
		throw new AssertionError("Payment for order " + orderId + " did not reach " + status + " within timeout");
	}

	@TestConfiguration
	static class CaptureConfig {

		@KafkaListener(id = CAPTURE_ID, topics = Topics.PAYMENT_EVENTS, groupId = "payment-flow-it-capture")
		void onPaymentEvent(EventEnvelope envelope) {
			PAYMENT_EVENTS.add(envelope);
		}
	}
}
