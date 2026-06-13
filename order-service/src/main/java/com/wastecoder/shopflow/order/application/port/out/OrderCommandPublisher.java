package com.wastecoder.shopflow.order.application.port.out;

import com.wastecoder.shopflow.order.domain.model.Order;

/**
 * Driven port for the saga commands the orchestrator sends to other services. The application speaks
 * in domain terms; the adapter owns the topic names, message types and payload assembly.
 */
public interface OrderCommandPublisher {

	void reserveStock(Order order);

	void processPayment(Order order);

	void releaseStock(Order order);
}
