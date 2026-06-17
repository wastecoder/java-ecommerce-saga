package com.wastecoder.shopflow.notification.adapter.web.dto.response;

import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(

		@Schema(description = "Notification identifier.", example = "a1b2c3d4-e5f6-4789-8abc-def012345678")
		UUID id,

		@Schema(description = "Order the notification refers to.", example = "9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f")
		UUID orderId,

		@Schema(description = "Customer notified.", example = "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d")
		UUID customerId,

		@Schema(description = "Kind of lifecycle notification.", example = "ORDER_CREATED")
		NotificationType type,

		@Schema(description = "Human-readable message that was \"sent\".",
				example = "Order 9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f received and is being processed.")
		String message,

		@Schema(description = "When the notification was recorded (UTC, ISO-8601).", example = "2026-06-16T12:34:56Z")
		Instant createdAt
) {

	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(notification.id(), notification.orderId(), notification.customerId(),
				notification.type(), notification.message(), notification.createdAt());
	}
}
