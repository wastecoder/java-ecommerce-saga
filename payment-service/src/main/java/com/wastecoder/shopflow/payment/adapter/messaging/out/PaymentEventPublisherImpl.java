package com.wastecoder.shopflow.payment.adapter.messaging.out;

import com.wastecoder.shopflow.payment.adapter.messaging.MessageType;
import com.wastecoder.shopflow.payment.adapter.messaging.Topics;
import com.wastecoder.shopflow.payment.adapter.messaging.payload.PaymentReplyPayload;
import com.wastecoder.shopflow.payment.application.port.out.EventPublisher;
import com.wastecoder.shopflow.payment.application.port.out.PaymentEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes the payment replies to {@code payment.events}, delegating to the generic {@link EventPublisher}.
 * Each reply is keyed by the order id (per-order ordering).
 */
@Component
public class PaymentEventPublisherImpl implements PaymentEventPublisher {

	private final EventPublisher eventPublisher;

	public PaymentEventPublisherImpl(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void paymentAuthorized(UUID orderId) {
		emit(MessageType.PAYMENT_AUTHORIZED, orderId);
	}

	@Override
	public void paymentFailed(UUID orderId) {
		emit(MessageType.PAYMENT_FAILED, orderId);
	}

	@Override
	public void paymentRefunded(UUID orderId) {
		emit(MessageType.PAYMENT_REFUNDED, orderId);
	}

	private void emit(String type, UUID orderId) {
		eventPublisher.publish(Topics.PAYMENT_EVENTS, orderId.toString(), type, new PaymentReplyPayload(orderId));
	}
}
