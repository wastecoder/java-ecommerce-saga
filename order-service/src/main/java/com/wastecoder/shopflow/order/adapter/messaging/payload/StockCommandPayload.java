package com.wastecoder.shopflow.order.adapter.messaging.payload;

import java.util.List;
import java.util.UUID;

/**
 * Payload for the ReserveStock and ReleaseStock commands sent to {@code inventory.commands}. Carries
 * the order id and the items (product + quantity) so inventory can reserve or reverse the exact lines.
 */
public record StockCommandPayload(UUID orderId, List<Item> items) {

	public record Item(UUID productId, int quantity) { }
}
