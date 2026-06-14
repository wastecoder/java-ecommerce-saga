package com.wastecoder.shopflow.inventory.adapter.messaging.payload;

import java.util.UUID;

/**
 * Payload for the stock replies emitted to {@code inventory.events} (StockReserved,
 * StockReservationFailed, StockReleased). The order-service routes by envelope type and ignores this
 * body; it is kept minimal but present so the envelope stays self-describing in the Kafka UI.
 */
public record StockReplyPayload(UUID orderId) { }
