package com.wastecoder.shopflow.order.adapter.messaging.out;

import com.wastecoder.shopflow.order.adapter.messaging.MessageType;
import com.wastecoder.shopflow.order.adapter.messaging.Topics;
import com.wastecoder.shopflow.order.adapter.messaging.payload.OrderEventPayload;
import com.wastecoder.shopflow.order.application.port.out.EventPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderEventPublisher;
import com.wastecoder.shopflow.order.domain.model.Order;
import org.springframework.stereotype.Component;

/**
 * Publishes the order facts to {@code order.events}, delegating to the generic {@link EventPublisher}.
 */
@Component
public class OrderEventPublisherImpl implements OrderEventPublisher {

	private final EventPublisher eventPublisher;

	public OrderEventPublisherImpl(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void orderCreated(Order order) {
		emit(MessageType.ORDER_CREATED, order);
	}

	@Override
	public void orderConfirmed(Order order) {
		emit(MessageType.ORDER_CONFIRMED, order);
	}

	@Override
	public void orderRejected(Order order) {
		emit(MessageType.ORDER_REJECTED, order);
	}

	@Override
	public void orderCancelled(Order order) {
		emit(MessageType.ORDER_CANCELLED, order);
	}

	private void emit(String type, Order order) {
		eventPublisher.publish(Topics.ORDER_EVENTS, order.id().toString(), type, OrderEventPayload.from(order));
	}
}
