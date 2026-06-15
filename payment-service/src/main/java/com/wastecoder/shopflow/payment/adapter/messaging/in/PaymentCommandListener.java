package com.wastecoder.shopflow.payment.adapter.messaging.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.shopflow.payment.adapter.messaging.Consumers;
import com.wastecoder.shopflow.payment.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.payment.adapter.messaging.MessageType;
import com.wastecoder.shopflow.payment.adapter.messaging.Topics;
import com.wastecoder.shopflow.payment.application.port.in.AuthorizePaymentUseCase;
import com.wastecoder.shopflow.payment.application.port.in.RefundPaymentUseCase;
import com.wastecoder.shopflow.payment.application.viewmodel.PaymentCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the saga commands from {@code payment.commands} and routes them by event type. Thin adapter: it
 * converts the type-agnostic envelope payload (a {@code Map}, since type headers are off) into a
 * {@link PaymentCommand} and delegates to the use cases, which own the business logic and the reply. Each
 * command is run through the {@link IdempotentMessageProcessor} so a redelivered {@code eventId} is handled
 * once; a malformed payload throws inside the transaction and is routed to the DLT.
 */
@Component
public class PaymentCommandListener {

	private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);

	private final AuthorizePaymentUseCase authorizePaymentUseCase;
	private final RefundPaymentUseCase refundPaymentUseCase;
	private final ObjectMapper objectMapper;
	private final IdempotentMessageProcessor idempotency;

	public PaymentCommandListener(AuthorizePaymentUseCase authorizePaymentUseCase,
			RefundPaymentUseCase refundPaymentUseCase, ObjectMapper objectMapper,
			IdempotentMessageProcessor idempotency) {
		this.authorizePaymentUseCase = authorizePaymentUseCase;
		this.refundPaymentUseCase = refundPaymentUseCase;
		this.objectMapper = objectMapper;
		this.idempotency = idempotency;
	}

	@KafkaListener(topics = Topics.PAYMENT_COMMANDS)
	public void onMessage(EventEnvelope envelope) {
		idempotency.processOnce(envelope.eventId(), Consumers.PAYMENT_COMMAND, () -> route(envelope));
	}

	private void route(EventEnvelope envelope) {
		switch (envelope.type()) {
			case MessageType.PROCESS_PAYMENT -> authorizePaymentUseCase.execute(toCommand(envelope));
			case MessageType.REFUND_PAYMENT -> refundPaymentUseCase.execute(toCommand(envelope));
			default -> log.warn("Ignoring unhandled payment command type '{}' for order {}", envelope.type(),
					envelope.orderId());
		}
	}

	private PaymentCommand toCommand(EventEnvelope envelope) {
		return objectMapper.convertValue(envelope.payload(), PaymentCommand.class);
	}
}
