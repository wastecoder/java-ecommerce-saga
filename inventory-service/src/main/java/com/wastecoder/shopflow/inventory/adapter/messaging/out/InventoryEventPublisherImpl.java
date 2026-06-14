package com.wastecoder.shopflow.inventory.adapter.messaging.out;

import com.wastecoder.shopflow.inventory.adapter.messaging.MessageType;
import com.wastecoder.shopflow.inventory.adapter.messaging.Topics;
import com.wastecoder.shopflow.inventory.adapter.messaging.payload.StockReplyPayload;
import com.wastecoder.shopflow.inventory.application.port.out.EventPublisher;
import com.wastecoder.shopflow.inventory.application.port.out.InventoryEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes the stock replies to {@code inventory.events}, delegating to the generic
 * {@link EventPublisher}. Each reply is keyed by the order id (per-order ordering).
 */
@Component
public class InventoryEventPublisherImpl implements InventoryEventPublisher {

	private final EventPublisher eventPublisher;

	public InventoryEventPublisherImpl(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void stockReserved(UUID orderId) {
		emit(MessageType.STOCK_RESERVED, orderId);
	}

	@Override
	public void stockReservationFailed(UUID orderId) {
		emit(MessageType.STOCK_RESERVATION_FAILED, orderId);
	}

	@Override
	public void stockReleased(UUID orderId) {
		emit(MessageType.STOCK_RELEASED, orderId);
	}

	private void emit(String type, UUID orderId) {
		eventPublisher.publish(Topics.INVENTORY_EVENTS, orderId.toString(), type, new StockReplyPayload(orderId));
	}
}
