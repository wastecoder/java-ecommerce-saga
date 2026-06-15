package com.wastecoder.shopflow.notification.adapter.messaging;

import com.wastecoder.shopflow.notification.TestcontainersConfiguration;
import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import com.wastecoder.shopflow.notification.testsupport.mother.OrderEventMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the real {@code order.events} listener end-to-end, simulating order-service by publishing the four
 * order events through the auto-configured {@link KafkaTemplate}, and asserts the recorded notification.
 * Covers the Fase 3 criterion (a notification is registered on confirm and cancel) and the created/rejected
 * events too.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationFlowIntegrationTest {

	@Autowired
	private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	@DisplayName("Given an OrderConfirmed event, when consumed, then a confirmed notification is recorded")
	void confirmed_recordsNotification() throws InterruptedException {
		Notification notification = publishAndAwait(MessageType.ORDER_CONFIRMED, NotificationType.ORDER_CONFIRMED);
		assertThat(notification.message()).contains("confirmed");
	}

	@Test
	@DisplayName("Given an OrderCancelled event, when consumed, then a cancelled notification is recorded")
	void cancelled_recordsNotification() throws InterruptedException {
		Notification notification = publishAndAwait(MessageType.ORDER_CANCELLED, NotificationType.ORDER_CANCELLED);
		assertThat(notification.message()).contains("cancelled");
	}

	@Test
	@DisplayName("Given an OrderCreated event, when consumed, then a created notification is recorded")
	void created_recordsNotification() throws InterruptedException {
		publishAndAwait(MessageType.ORDER_CREATED, NotificationType.ORDER_CREATED);
	}

	@Test
	@DisplayName("Given an OrderRejected event, when consumed, then a rejected notification is recorded")
	void rejected_recordsNotification() throws InterruptedException {
		publishAndAwait(MessageType.ORDER_REJECTED, NotificationType.ORDER_REJECTED);
	}

	private Notification publishAndAwait(String eventType, NotificationType expected) throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		Map<String, Object> payload = OrderEventMother.aPayloadMap(orderId, customerId, new BigDecimal("100.00"),
				expected.name());
		EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), eventType, orderId.toString(), Instant.now(),
				payload);

		kafkaTemplate.send(Topics.ORDER_EVENTS, orderId.toString(), envelope);

		Notification notification = await(orderId);
		assertThat(notification.type()).isEqualTo(expected);
		assertThat(notification.orderId()).isEqualTo(orderId);
		assertThat(notification.customerId()).isEqualTo(customerId);
		return notification;
	}

	private Notification await(UUID orderId) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
		while (System.nanoTime() < deadline) {
			List<Notification> found = notificationRepository.findByOrderId(orderId);
			if (!found.isEmpty()) {
				return found.get(0);
			}
			Thread.sleep(200);
		}
		throw new AssertionError("No notification recorded for order " + orderId + " within timeout");
	}
}
