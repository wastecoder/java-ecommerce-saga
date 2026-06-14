package com.wastecoder.shopflow.inventory.application.port.in;

import com.wastecoder.shopflow.inventory.application.viewmodel.StockCommand;

public interface ReserveStockUseCase {

	void execute(StockCommand command);
}
