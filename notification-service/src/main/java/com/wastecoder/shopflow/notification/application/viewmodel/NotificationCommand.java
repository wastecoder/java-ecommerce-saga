package com.wastecoder.shopflow.notification.application.viewmodel;

import com.wastecoder.shopflow.notification.domain.model.NotificationType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Boundary input to record a notification, built by the messaging adapter from an order event consumed on
 * {@code order.events}.
 */
public record NotificationCommand(UUID orderId, UUID customerId, NotificationType type, BigDecimal totalAmount) {
}
