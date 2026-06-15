package com.wastecoder.shopflow.notification.adapter.messaging.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.shopflow.notification.adapter.messaging.Consumers;
import com.wastecoder.shopflow.notification.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.notification.adapter.messaging.MessageType;
import com.wastecoder.shopflow.notification.adapter.messaging.Topics;
import com.wastecoder.shopflow.notification.adapter.messaging.payload.OrderEventPayload;
import com.wastecoder.shopflow.notification.application.port.in.RecordNotificationUseCase;
import com.wastecoder.shopflow.notification.application.viewmodel.NotificationCommand;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the order facts from {@code order.events} and records a notification for each. Thin adapter: it
 * maps the envelope {@code type} to a {@link NotificationType}, converts the type-agnostic payload (a
 * {@code Map}, since type headers are off) into an {@link OrderEventPayload} and delegates to the use case,
 * which owns the persistence and the mock "send". Each event is run through the
 * {@link IdempotentMessageProcessor} so a redelivered {@code eventId} is recorded once; a malformed payload
 * throws inside the transaction and is routed to the DLT.
 */
@Component
public class OrderEventListener {

	private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

	private final RecordNotificationUseCase recordNotificationUseCase;
	private final ObjectMapper objectMapper;
	private final IdempotentMessageProcessor idempotency;

	public OrderEventListener(RecordNotificationUseCase recordNotificationUseCase, ObjectMapper objectMapper,
			IdempotentMessageProcessor idempotency) {
		this.recordNotificationUseCase = recordNotificationUseCase;
		this.objectMapper = objectMapper;
		this.idempotency = idempotency;
	}

	@KafkaListener(topics = Topics.ORDER_EVENTS)
	public void onMessage(EventEnvelope envelope) {
		idempotency.processOnce(envelope.eventId(), Consumers.ORDER_EVENT, () -> route(envelope));
	}

	private void route(EventEnvelope envelope) {
		NotificationType type = toNotificationType(envelope.type());
		if (type == null) {
			log.warn("Ignoring unhandled order event type '{}' for order {}", envelope.type(), envelope.orderId());
			return;
		}
		OrderEventPayload payload = objectMapper.convertValue(envelope.payload(), OrderEventPayload.class);
		recordNotificationUseCase.execute(
				new NotificationCommand(payload.orderId(), payload.customerId(), type, payload.totalAmount()));
	}

	private NotificationType toNotificationType(String eventType) {
		return switch (eventType) {
			case MessageType.ORDER_CREATED -> NotificationType.ORDER_CREATED;
			case MessageType.ORDER_CONFIRMED -> NotificationType.ORDER_CONFIRMED;
			case MessageType.ORDER_REJECTED -> NotificationType.ORDER_REJECTED;
			case MessageType.ORDER_CANCELLED -> NotificationType.ORDER_CANCELLED;
			default -> null;
		};
	}
}
