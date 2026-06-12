package com.wastecoder.shopflow.inventory.application.port.in;

import com.wastecoder.shopflow.inventory.domain.model.StockItem;

import java.util.List;

public interface ListStockUseCase {

	List<StockItem> execute();
}
