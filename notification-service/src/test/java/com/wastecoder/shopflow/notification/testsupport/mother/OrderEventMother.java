package com.wastecoder.shopflow.notification.testsupport.mother;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * The raw envelope-payload {@code Map} shape, mirroring order-service's {@code OrderEventPayload(orderId,
 * customerId, totalAmount, status)}, as the consumer receives it after JSON deserialization (type headers
 * are off, so the payload arrives as a {@code Map}).
 */
public final class OrderEventMother {

	public static final UUID ORDER_ID = NotificationMother.ORDER_ID;
	public static final UUID CUSTOMER_ID = NotificationMother.CUSTOMER_ID;
	public static final BigDecimal TOTAL_AMOUNT = NotificationMother.TOTAL_AMOUNT;

	private OrderEventMother() {
	}

	public static Map<String, Object> aPayloadMap(String status) {
		return aPayloadMap(ORDER_ID, CUSTOMER_ID, TOTAL_AMOUNT, status);
	}

	public static Map<String, Object> aPayloadMap(UUID orderId, UUID customerId, BigDecimal totalAmount,
			String status) {
		return Map.of(
				"orderId", orderId.toString(),
				"customerId", customerId.toString(),
				"totalAmount", totalAmount,
				"status", status);
	}
}
