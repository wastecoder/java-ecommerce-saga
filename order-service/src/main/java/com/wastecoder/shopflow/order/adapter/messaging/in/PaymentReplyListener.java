package com.wastecoder.shopflow.order.adapter.messaging.in;

import com.wastecoder.shopflow.order.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.order.adapter.messaging.MessageType;
import com.wastecoder.shopflow.order.adapter.messaging.Topics;
import com.wastecoder.shopflow.order.application.port.in.HandlePaymentReplyUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes payment replies from {@code payment.events} and routes them to the saga by event type.
 * Thin adapter: no business logic, no payload parsing.
 */
@Component
public class PaymentReplyListener {

	private static final Logger log = LoggerFactory.getLogger(PaymentReplyListener.class);

	private final HandlePaymentReplyUseCase useCase;

	public PaymentReplyListener(HandlePaymentReplyUseCase useCase) {
		this.useCase = useCase;
	}

	@KafkaListener(topics = Topics.PAYMENT_EVENTS)
	public void onMessage(EventEnvelope envelope) {
		UUID orderId = UUID.fromString(envelope.orderId());
		switch (envelope.type()) {
			case MessageType.PAYMENT_AUTHORIZED -> useCase.onPaymentAuthorized(orderId);
			case MessageType.PAYMENT_FAILED -> useCase.onPaymentFailed(orderId);
			default -> log.warn("Ignoring unhandled payment event type '{}' for order {}", envelope.type(), orderId);
		}
	}
}
