package com.wastecoder.shopflow.notification.adapter.persistence.mapper;

import com.wastecoder.shopflow.notification.adapter.persistence.entity.NotificationEntity;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import com.wastecoder.shopflow.notification.testsupport.mother.NotificationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEntityMapperTest {

	private final NotificationEntityMapper mapper = new NotificationEntityMapperImpl();

	@Test
	@DisplayName("Given null inputs, when mapping, then null is returned")
	void nullInputs_returnNull() {
		assertThat(mapper.toEntity(null)).isNull();
		assertThat(mapper.toDomain(null)).isNull();
	}

	@Test
	@DisplayName("Given a notification, when mapped to entity and back, then all fields round-trip")
	void roundTrip() {
		Notification domain = NotificationMother.aConfirmedNotification();

		NotificationEntity entity = mapper.toEntity(domain);
		assertThat(entity.getId()).isEqualTo(domain.id());
		assertThat(entity.getOrderId()).isEqualTo(domain.orderId());
		assertThat(entity.getCustomerId()).isEqualTo(domain.customerId());
		assertThat(entity.getType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
		assertThat(entity.getMessage()).isEqualTo(domain.message());
		assertThat(entity.getCreatedAt()).isEqualTo(domain.createdAt());

		assertThat(mapper.toDomain(entity)).isEqualTo(domain);
	}
}
