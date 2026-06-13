package com.wastecoder.shopflow.order.application.port.in;

import java.util.UUID;

/**
 * Driving port for inventory replies consumed from {@code inventory.events}. The messaging adapter
 * routes by event type and calls the matching method; the saga coordinator implements it.
 */
public interface HandleStockReplyUseCase {

	void onStockReserved(UUID orderId);

	void onStockReservationFailed(UUID orderId);

	void onStockReleased(UUID orderId);
}
