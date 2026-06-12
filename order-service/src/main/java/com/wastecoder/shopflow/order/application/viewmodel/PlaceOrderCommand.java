package com.wastecoder.shopflow.order.application.viewmodel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(

		UUID customerId,

		List<Item> items
) {

	public record Item(
			UUID productId,
			int quantity,
			BigDecimal unitPrice
	) { }
}
