package com.wastecoder.shopflow.order.application.port.out;

/**
 * Driven port for publishing messages to the message broker.
 *
 * <p>Framework-agnostic on purpose: it speaks topic/key/type plus an arbitrary payload, and the
 * adapter is responsible for wrapping them into the wire envelope and sending them. The {@code key}
 * is the partition key (the order id), which guarantees per-order ordering.
 */
public interface EventPublisher {

	void publish(String topic, String key, String type, Object payload);
}
