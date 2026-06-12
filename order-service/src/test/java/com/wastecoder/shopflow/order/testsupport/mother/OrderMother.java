package com.wastecoder.shopflow.order.testsupport.mother;

import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderItem;
import com.wastecoder.shopflow.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderMother {

	public static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	public static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	public static final UUID ITEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	public static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	public static final UUID SECOND_ITEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
	public static final UUID SECOND_PRODUCT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
	public static final Instant CREATED_AT = Instant.parse("2026-06-12T12:00:00Z");

	private OrderMother() {
	}

	public static OrderItem aValidItem() {
		return new OrderItem(ITEM_ID, PRODUCT_ID, 2, new BigDecimal("10.50"));
	}

	public static OrderItem aSecondItem() {
		return new OrderItem(SECOND_ITEM_ID, SECOND_PRODUCT_ID, 1, new BigDecimal("4.00"));
	}

	/** A persisted PENDING order with a single item; totalAmount = 2 * 10.50 = 21.00. */
	public static Order aPendingOrder() {
		return new Order(
				ORDER_ID,
				CUSTOMER_ID,
				OrderStatus.PENDING,
				new BigDecimal("21.00"),
				CREATED_AT,
				List.of(aValidItem()));
	}
}
