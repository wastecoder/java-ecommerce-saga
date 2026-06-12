package com.wastecoder.shopflow.inventory.application.port.in;

import com.wastecoder.shopflow.inventory.application.viewmodel.SeedResult;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;

import java.util.List;

public interface SeedStockUseCase {

	SeedResult execute(List<StockItem> items);
}
