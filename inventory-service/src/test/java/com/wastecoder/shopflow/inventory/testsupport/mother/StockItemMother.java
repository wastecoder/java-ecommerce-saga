package com.wastecoder.shopflow.inventory.testsupport.mother;

import com.wastecoder.shopflow.inventory.domain.model.StockItem;

import java.util.UUID;

public final class StockItemMother {

	public static final UUID PRODUCT_ID = UUID.fromString("a1111111-1111-1111-1111-111111111111");
	public static final UUID OTHER_PRODUCT_ID = UUID.fromString("a2222222-2222-2222-2222-222222222222");

	private StockItemMother() {
	}

	public static StockItem aStockItem() {
		return new StockItem(PRODUCT_ID, 100, 0);
	}

	public static StockItem aStockItemFor(UUID productId, int available) {
		return new StockItem(productId, available, 0);
	}
}
