package com.wastecoder.shopflow.order.adapter.web.dto.response;

import com.wastecoder.shopflow.order.domain.model.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(

		@Schema(description = "Order item identifier.", example = "1a2b3c4d-5e6f-4071-8233-445566778899")
		UUID id,

		@Schema(description = "Product ordered.", example = "7d2f0f9e-2c1a-4f5b-9c3d-1e2a3b4c5d6e")
		UUID productId,

		@Schema(description = "Units ordered.", example = "2")
		int quantity,

		@Schema(description = "Price per unit at order time.", example = "10.50")
		BigDecimal unitPrice
) {

	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(item.id(), item.productId(), item.quantity(), item.unitPrice());
	}
}
