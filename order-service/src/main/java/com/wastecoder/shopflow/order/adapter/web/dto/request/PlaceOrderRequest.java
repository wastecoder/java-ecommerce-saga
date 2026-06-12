package com.wastecoder.shopflow.order.adapter.web.dto.request;

import com.wastecoder.shopflow.order.application.viewmodel.PlaceOrderCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(

		@NotNull(message = "customerId must not be null")
		@Schema(example = "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d")
		UUID customerId,

		@NotEmpty(message = "items must not be empty")
		@Valid
		List<OrderItemRequest> items
) {

	public PlaceOrderCommand toCommand() {
		List<PlaceOrderCommand.Item> commandItems = items.stream()
				.map(item -> new PlaceOrderCommand.Item(item.productId(), item.quantity(), item.unitPrice()))
				.toList();
		return new PlaceOrderCommand(customerId, commandItems);
	}
}
