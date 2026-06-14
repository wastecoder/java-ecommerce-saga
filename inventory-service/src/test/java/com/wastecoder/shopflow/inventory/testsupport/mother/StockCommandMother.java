package com.wastecoder.shopflow.inventory.testsupport.mother;

import com.wastecoder.shopflow.inventory.application.viewmodel.StockCommand;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StockCommandMother {

	public static final UUID ORDER_ID = StockReservationMother.ORDER_ID;

	private StockCommandMother() {
	}

	public static StockCommand aValidCommand() {
		return new StockCommand(ORDER_ID, List.of(new StockCommand.Item(StockItemMother.PRODUCT_ID, 2)));
	}

	public static StockCommand aTwoItemCommand() {
		return new StockCommand(ORDER_ID, List.of(
				new StockCommand.Item(StockItemMother.PRODUCT_ID, 2),
				new StockCommand.Item(StockItemMother.OTHER_PRODUCT_ID, 1)));
	}

	public static StockCommand aCommandRequesting(UUID productId, int quantity) {
		return new StockCommand(ORDER_ID, List.of(new StockCommand.Item(productId, quantity)));
	}

	public static StockCommand anEmptyCommand() {
		return new StockCommand(ORDER_ID, List.of());
	}

	/** The raw envelope-payload {@code Map} shape, as the consumer receives it after JSON deserialization. */
	public static Map<String, Object> asPayloadMap(StockCommand command) {
		List<Map<String, Object>> items = command.items().stream()
				.map(item -> Map.<String, Object>of(
						"productId", item.productId().toString(),
						"quantity", item.quantity()))
				.toList();
		return Map.of("orderId", command.orderId().toString(), "items", items);
	}
}
