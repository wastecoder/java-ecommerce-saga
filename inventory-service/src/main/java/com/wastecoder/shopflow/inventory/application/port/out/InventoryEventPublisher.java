package com.wastecoder.shopflow.inventory.application.port.out;

import java.util.UUID;

/**
 * Driven port for the stock replies inventory emits to {@code inventory.events}. The application speaks
 * in domain terms (the order id); the adapter owns the topic, message types and payload assembly.
 */
public interface InventoryEventPublisher {

	void stockReserved(UUID orderId);

	void stockReservationFailed(UUID orderId);

	void stockReleased(UUID orderId);
}
