package com.wastecoder.shopflow.order.domain.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(

		@NotNull(message = "item id must not be null")
		UUID id,

		@NotNull(message = "productId must not be null")
		UUID productId,

		@Positive(message = "quantity must be positive")
		int quantity,

		@NotNull(message = "unitPrice must not be null")
		@Positive(message = "unitPrice must be positive")
		BigDecimal unitPrice
) { }
