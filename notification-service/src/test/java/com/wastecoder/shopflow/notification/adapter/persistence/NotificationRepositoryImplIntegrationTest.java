package com.wastecoder.shopflow.notification.adapter.persistence;

import com.wastecoder.shopflow.notification.TestcontainersConfiguration;
import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationRepositoryImplIntegrationTest {

	@Autowired
	private NotificationRepository repository;

	@Test
	@DisplayName("Given a notification, when saved and read back by order id, then all fields are preserved")
	void saveAndFindByOrderId_roundTrips() {
		UUID orderId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-06-15T10:30:00.123456Z");
		Notification notification = Notification.create(orderId, customerId, NotificationType.ORDER_CONFIRMED,
				"Order confirmed message", createdAt);

		repository.save(notification);

		List<Notification> found = repository.findByOrderId(orderId);
		assertThat(found).hasSize(1);
		Notification persisted = found.get(0);
		assertThat(persisted.id()).isEqualTo(notification.id());
		assertThat(persisted.orderId()).isEqualTo(orderId);
		assertThat(persisted.customerId()).isEqualTo(customerId);
		assertThat(persisted.type()).isEqualTo(NotificationType.ORDER_CONFIRMED);
		assertThat(persisted.message()).isEqualTo("Order confirmed message");
		assertThat(persisted.createdAt()).isEqualTo(createdAt);
	}

	@Test
	@DisplayName("Given several notifications for one order, when read by order id, then they come back most recent first")
	void findByOrderId_returnsMostRecentFirst() {
		UUID orderId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		repository.save(Notification.create(orderId, customerId, NotificationType.ORDER_CREATED, "created",
				Instant.parse("2026-06-15T10:00:00Z")));
		repository.save(Notification.create(orderId, customerId, NotificationType.ORDER_CONFIRMED, "confirmed",
				Instant.parse("2026-06-15T11:00:00Z")));

		assertThat(repository.findByOrderId(orderId)).extracting(Notification::type)
				.containsExactly(NotificationType.ORDER_CONFIRMED, NotificationType.ORDER_CREATED);
	}

	@Test
	@DisplayName("Given no notifications for an order, when querying by order id, then the result is empty")
	void findByOrderId_emptyWhenAbsent() {
		assertThat(repository.findByOrderId(UUID.randomUUID())).isEmpty();
	}

	@Test
	@DisplayName("Given recorded notifications, when listing all, then they are ordered most recent first")
	void findAll_returnsNotificationsMostRecentFirst() {
		repository.save(Notification.create(UUID.randomUUID(), UUID.randomUUID(), NotificationType.ORDER_CREATED,
				"any", Instant.parse("2026-06-15T09:00:00Z")));

		List<Notification> all = repository.findAll();

		assertThat(all).isNotEmpty();
		assertThat(all).isSortedAccordingTo(Comparator.comparing(Notification::createdAt).reversed());
	}
}
