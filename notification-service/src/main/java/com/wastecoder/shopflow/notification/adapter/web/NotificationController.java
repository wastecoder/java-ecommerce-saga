package com.wastecoder.shopflow.notification.adapter.web;

import com.wastecoder.shopflow.notification.adapter.web.dto.response.NotificationResponse;
import com.wastecoder.shopflow.notification.application.port.in.GetNotificationsByOrderUseCase;
import com.wastecoder.shopflow.notification.application.port.in.ListNotificationsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Endpoints", description = "Operations to query the notifications recorded for orders.")
public class NotificationController {

	private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

	private final ListNotificationsUseCase listNotificationsUseCase;
	private final GetNotificationsByOrderUseCase getNotificationsByOrderUseCase;

	public NotificationController(ListNotificationsUseCase listNotificationsUseCase,
			GetNotificationsByOrderUseCase getNotificationsByOrderUseCase) {
		this.listNotificationsUseCase = listNotificationsUseCase;
		this.getNotificationsByOrderUseCase = getNotificationsByOrderUseCase;
	}

	@GetMapping
	@Operation(summary = "List all notifications",
			description = "Returns every recorded notification, most recent first.")
	@ApiResponse(responseCode = "200", description = "Notifications listed")
	public ResponseEntity<List<NotificationResponse>> list() {
		log.info("Listing notifications");
		return ResponseEntity.ok(listNotificationsUseCase.execute().stream().map(NotificationResponse::from).toList());
	}

	@GetMapping("/{orderId}")
	@Operation(summary = "List the notifications of an order",
			description = "Returns the notifications recorded for the given order id, most recent first "
					+ "(an empty list when none exist).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Notifications listed"),
			@ApiResponse(responseCode = "400", description = "Invalid order id")
	})
	public ResponseEntity<List<NotificationResponse>> getByOrderId(@PathVariable UUID orderId) {
		log.info("Listing notifications for order={}", orderId);
		return ResponseEntity.ok(
				getNotificationsByOrderUseCase.execute(orderId).stream().map(NotificationResponse::from).toList());
	}
}
