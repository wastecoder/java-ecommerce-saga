package com.wastecoder.shopflow.inventory.domain.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record StockItem(

		@NotNull(message = "productId must not be null")
		UUID productId,

		@PositiveOrZero(message = "available must not be negative")
		int available,

		@PositiveOrZero(message = "reserved must not be negative")
		int reserved
) { }
