package com.wastecoder.shopflow.notification.adapter.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload of the order facts emitted to {@code order.events} (OrderCreated, OrderConfirmed, OrderRejected,
 * OrderCancelled), as this service receives it after JSON deserialization. Mirrors order-service's
 * {@code OrderEventPayload} so the consumer stays self-contained; the listener converts the type-agnostic
 * envelope payload (a {@code Map}, since type headers are off) into this record.
 */
public record OrderEventPayload(UUID orderId, UUID customerId, BigDecimal totalAmount, String status) {
}
