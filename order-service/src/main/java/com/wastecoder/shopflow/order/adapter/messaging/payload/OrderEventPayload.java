package com.wastecoder.shopflow.order.adapter.messaging.payload;

import com.wastecoder.shopflow.order.domain.model.Order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload for the order facts emitted to {@code order.events}: OrderCreated, OrderConfirmed,
 * OrderRejected and OrderCancelled. The {@code status} mirrors the order's status so consumers
 * (e.g. notification-service) are self-contained.
 */
public record OrderEventPayload(UUID orderId, UUID customerId, BigDecimal totalAmount, String status) {

	public static OrderEventPayload from(Order order) {
		return new OrderEventPayload(order.id(), order.customerId(), order.totalAmount(), order.status().name());
	}
}
