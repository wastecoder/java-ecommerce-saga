package com.wastecoder.shopflow.order.domain.model;

import com.wastecoder.shopflow.order.testsupport.mother.OrderMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

	@Test
	@DisplayName("Given valid items, when an order is placed, then it is PENDING with the summed total amount")
	void place_setsPendingStatusAndComputesTotal() {
		List<OrderItem> items = List.of(OrderMother.aValidItem());

		Order order = Order.place(OrderMother.ORDER_ID, OrderMother.CUSTOMER_ID, items, OrderMother.CREATED_AT);

		assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
		assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("21.00"));
		assertThat(order.id()).isEqualTo(OrderMother.ORDER_ID);
		assertThat(order.customerId()).isEqualTo(OrderMother.CUSTOMER_ID);
		assertThat(order.createdAt()).isEqualTo(OrderMother.CREATED_AT);
		assertThat(order.items()).containsExactlyElementsOf(items);
	}

	@Test
	@DisplayName("Given multiple items, when an order is placed, then the total amount sums quantity times unit price")
	void place_sumsTotalAcrossItems() {
		List<OrderItem> items = List.of(OrderMother.aValidItem(), OrderMother.aSecondItem());

		Order order = Order.place(UUID.randomUUID(), OrderMother.CUSTOMER_ID, items, Instant.now());

		assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
	}
}
