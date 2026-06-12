package com.wastecoder.shopflow.order.adapter.web.dto.response;

import com.wastecoder.shopflow.order.domain.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(

		UUID id,

		UUID productId,

		int quantity,

		BigDecimal unitPrice
) {

	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(item.id(), item.productId(), item.quantity(), item.unitPrice());
	}
}
