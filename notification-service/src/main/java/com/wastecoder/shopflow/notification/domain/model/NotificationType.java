package com.wastecoder.shopflow.notification.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The kind of customer notification raised for an order-lifecycle event consumed from {@code order.events}.
 * Each constant renders the human-readable message that the (mock) channel "sends"; {@code totalAmount} is
 * used only where it is relevant to the message.
 */
public enum NotificationType {

	ORDER_CREATED {
		@Override
		public String message(UUID orderId, BigDecimal totalAmount) {
			return "Order " + orderId + " received and is being processed.";
		}
	},
	ORDER_CONFIRMED {
		@Override
		public String message(UUID orderId, BigDecimal totalAmount) {
			return "Order " + orderId + " confirmed (total " + totalAmount + ").";
		}
	},
	ORDER_REJECTED {
		@Override
		public String message(UUID orderId, BigDecimal totalAmount) {
			return "Order " + orderId + " rejected: out of stock.";
		}
	},
	ORDER_CANCELLED {
		@Override
		public String message(UUID orderId, BigDecimal totalAmount) {
			return "Order " + orderId + " cancelled: payment failed.";
		}
	};

	/** Renders the notification message for the given order. */
	public abstract String message(UUID orderId, BigDecimal totalAmount);
}
