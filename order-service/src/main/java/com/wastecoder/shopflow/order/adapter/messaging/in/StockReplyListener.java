package com.wastecoder.shopflow.order.adapter.messaging.in;

import com.wastecoder.shopflow.order.adapter.messaging.Consumers;
import com.wastecoder.shopflow.order.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.order.adapter.messaging.MessageType;
import com.wastecoder.shopflow.order.adapter.messaging.Topics;
import com.wastecoder.shopflow.order.application.port.in.HandleStockReplyUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes inventory replies from {@code inventory.events} and routes them to the saga by event type.
 * Thin adapter: no business logic, no payload parsing — the envelope carries the order id and type. Each
 * reply is run through the {@link IdempotentMessageProcessor} so a redelivered {@code eventId} is handled
 * once; a malformed {@code orderId} throws inside the transaction and is routed to the DLT.
 */
@Component
public class StockReplyListener {

	private static final Logger log = LoggerFactory.getLogger(StockReplyListener.class);

	private final HandleStockReplyUseCase useCase;
	private final IdempotentMessageProcessor idempotency;

	public StockReplyListener(HandleStockReplyUseCase useCase, IdempotentMessageProcessor idempotency) {
		this.useCase = useCase;
		this.idempotency = idempotency;
	}

	@KafkaListener(topics = Topics.INVENTORY_EVENTS)
	public void onMessage(EventEnvelope envelope) {
		idempotency.processOnce(envelope.eventId(), Consumers.STOCK_REPLY, () -> route(envelope));
	}

	private void route(EventEnvelope envelope) {
		UUID orderId = UUID.fromString(envelope.orderId());
		switch (envelope.type()) {
			case MessageType.STOCK_RESERVED -> useCase.onStockReserved(orderId);
			case MessageType.STOCK_RESERVATION_FAILED -> useCase.onStockReservationFailed(orderId);
			case MessageType.STOCK_RELEASED -> useCase.onStockReleased(orderId);
			default -> log.warn("Ignoring unhandled inventory event type '{}' for order {}", envelope.type(), orderId);
		}
	}
}
