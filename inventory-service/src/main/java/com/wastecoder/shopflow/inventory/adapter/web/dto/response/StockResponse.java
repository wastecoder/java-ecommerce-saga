package com.wastecoder.shopflow.inventory.adapter.web.dto.response;

import com.wastecoder.shopflow.inventory.domain.model.StockItem;

import java.util.UUID;

public record StockResponse(

		UUID productId,

		int available,

		int reserved
) {

	public static StockResponse from(StockItem stockItem) {
		return new StockResponse(stockItem.productId(), stockItem.available(), stockItem.reserved());
	}
}
