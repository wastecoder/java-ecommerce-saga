package com.wastecoder.shopflow.order.adapter.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(

		@NotNull(message = "productId must not be null")
		@Schema(example = "7d2f0f9e-2c1a-4f5b-9c3d-1e2a3b4c5d6e")
		UUID productId,

		@Positive(message = "quantity must be positive")
		@Schema(example = "2")
		int quantity,

		@NotNull(message = "unitPrice must not be null")
		@Positive(message = "unitPrice must be positive")
		@Schema(example = "10.50")
		BigDecimal unitPrice
) { }
