package com.wastecoder.shopflow.inventory.adapter.web.dto.response;

import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StockResponse(

		@Schema(description = "Product the stock belongs to.", example = "7d2f0f9e-2c1a-4f5b-9c3d-1e2a3b4c5d6e")
		UUID productId,

		@Schema(description = "Units available to reserve.", example = "100")
		int available,

		@Schema(description = "Units currently reserved.", example = "5")
		int reserved
) {

	public static StockResponse from(StockItem stockItem) {
		return new StockResponse(stockItem.productId(), stockItem.available(), stockItem.reserved());
	}
}
