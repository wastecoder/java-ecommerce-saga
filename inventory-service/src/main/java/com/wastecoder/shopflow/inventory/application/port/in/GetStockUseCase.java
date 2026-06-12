package com.wastecoder.shopflow.inventory.application.port.in;

import com.wastecoder.shopflow.inventory.domain.model.StockItem;

import java.util.UUID;

public interface GetStockUseCase {

	StockItem execute(UUID productId);
}
