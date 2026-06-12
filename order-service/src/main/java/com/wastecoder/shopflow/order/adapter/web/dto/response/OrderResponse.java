package com.wastecoder.shopflow.order.adapter.web.dto.response;

import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(

		UUID id,

		UUID customerId,

		OrderStatus status,

		BigDecimal totalAmount,

		Instant createdAt,

		List<OrderItemResponse> items
) {

	public static OrderResponse from(Order order) {
		List<OrderItemResponse> items = order.items().stream()
				.map(OrderItemResponse::from)
				.toList();
		return new OrderResponse(
				order.id(),
				order.customerId(),
				order.status(),
				order.totalAmount(),
				order.createdAt(),
				items
		);
	}
}
