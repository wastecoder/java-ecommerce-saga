package com.wastecoder.shopflow.notification.adapter.web.dto.response;

import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(

		UUID id,

		UUID orderId,

		UUID customerId,

		NotificationType type,

		String message,

		Instant createdAt
) {

	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(notification.id(), notification.orderId(), notification.customerId(),
				notification.type(), notification.message(), notification.createdAt());
	}
}
