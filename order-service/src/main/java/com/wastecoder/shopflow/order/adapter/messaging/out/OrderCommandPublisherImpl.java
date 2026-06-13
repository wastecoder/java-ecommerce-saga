package com.wastecoder.shopflow.order.adapter.messaging.out;

import com.wastecoder.shopflow.order.adapter.messaging.MessageType;
import com.wastecoder.shopflow.order.adapter.messaging.Topics;
import com.wastecoder.shopflow.order.adapter.messaging.payload.ProcessPaymentPayload;
import com.wastecoder.shopflow.order.adapter.messaging.payload.StockCommandPayload;
import com.wastecoder.shopflow.order.application.port.out.EventPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderCommandPublisher;
import com.wastecoder.shopflow.order.domain.model.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publishes the saga commands to inventory/payment, building each payload from the order and delegating
 * to the generic {@link EventPublisher} (which owns the envelope and the {@code KafkaTemplate}).
 */
@Component
public class OrderCommandPublisherImpl implements OrderCommandPublisher {

	private final EventPublisher eventPublisher;

	public OrderCommandPublisherImpl(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void reserveStock(Order order) {
		eventPublisher.publish(Topics.INVENTORY_COMMANDS, key(order), MessageType.RESERVE_STOCK, stockPayload(order));
	}

	@Override
	public void processPayment(Order order) {
		ProcessPaymentPayload payload = new ProcessPaymentPayload(order.id(), order.customerId(), order.totalAmount());
		eventPublisher.publish(Topics.PAYMENT_COMMANDS, key(order), MessageType.PROCESS_PAYMENT, payload);
	}

	@Override
	public void releaseStock(Order order) {
		eventPublisher.publish(Topics.INVENTORY_COMMANDS, key(order), MessageType.RELEASE_STOCK, stockPayload(order));
	}

	private StockCommandPayload stockPayload(Order order) {
		List<StockCommandPayload.Item> items = order.items().stream()
				.map(item -> new StockCommandPayload.Item(item.productId(), item.quantity()))
				.toList();
		return new StockCommandPayload(order.id(), items);
	}

	private String key(Order order) {
		return order.id().toString();
	}
}
