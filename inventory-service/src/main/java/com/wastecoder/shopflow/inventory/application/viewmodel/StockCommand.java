package com.wastecoder.shopflow.inventory.application.viewmodel;

import java.util.List;
import java.util.UUID;

/**
 * Boundary command unpacked from the {@code inventory.commands} envelope payload (ReserveStock /
 * ReleaseStock). Field names mirror the order-service wire payload ({@code orderId}, {@code items},
 * {@code productId}, {@code quantity}) so Jackson can convert the raw envelope {@code Map} into this
 * type. Shared by both the reserve and release use cases.
 */
public record StockCommand(

		UUID orderId,

		List<Item> items
) {

	public record Item(UUID productId, int quantity) { }
}
