package com.wastecoder.shopflow.order.application.port.out;

import com.wastecoder.shopflow.order.domain.model.Order;

/**
 * Driven port for the domain facts the orchestrator emits about its own orders to {@code order.events}.
 * The application speaks in domain terms; the adapter owns the topic, message types and payloads.
 */
public interface OrderEventPublisher {

	void orderCreated(Order order);

	void orderConfirmed(Order order);

	void orderRejected(Order order);

	void orderCancelled(Order order);
}
