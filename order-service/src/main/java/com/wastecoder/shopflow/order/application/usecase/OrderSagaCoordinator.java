package com.wastecoder.shopflow.order.application.usecase;

import com.wastecoder.shopflow.order.application.port.in.HandlePaymentReplyUseCase;
import com.wastecoder.shopflow.order.application.port.in.HandleStockReplyUseCase;
import com.wastecoder.shopflow.order.application.port.out.OrderCommandPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderEventPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.domain.exception.InvalidOrderStateException;
import com.wastecoder.shopflow.order.domain.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Orchestrates the order SAGA. It reacts to stock and payment replies, advances the order through its
 * states and compensates when payment fails.
 *
 * <p>Each reply loads the order, applies a domain transition and then publishes the next command or
 * terminal event. A reply for an unknown order, or one whose transition is illegal for the current
 * state (a duplicate or out-of-order delivery), is logged and ignored — no state change, no publish.
 * This gives partial idempotency via domain state guards until the {@code processed_messages} table
 * and DLT arrive (item 4). DB write and Kafka publish are not atomic in the MVP (Transactional Outbox
 * is future work); operations are ordered save-then-publish.
 */
@Service
public class OrderSagaCoordinator implements HandleStockReplyUseCase, HandlePaymentReplyUseCase {

	private static final Logger log = LoggerFactory.getLogger(OrderSagaCoordinator.class);

	private final OrderRepository repository;
	private final OrderCommandPublisher commandPublisher;
	private final OrderEventPublisher eventPublisher;

	public OrderSagaCoordinator(OrderRepository repository, OrderCommandPublisher commandPublisher,
			OrderEventPublisher eventPublisher) {
		this.repository = repository;
		this.commandPublisher = commandPublisher;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void onStockReserved(UUID orderId) {
		advance(orderId, "StockReserved", order -> {
			Order reserved = order.markStockReserved();
			repository.save(reserved);
			commandPublisher.processPayment(reserved);
		});
	}

	@Override
	public void onStockReservationFailed(UUID orderId) {
		advance(orderId, "StockReservationFailed", order -> {
			Order rejected = order.reject();
			repository.save(rejected);
			eventPublisher.orderRejected(rejected);
		});
	}

	@Override
	public void onPaymentAuthorized(UUID orderId) {
		advance(orderId, "PaymentAuthorized", order -> {
			Order confirmed = order.markPaid().confirm();
			repository.save(confirmed);
			eventPublisher.orderConfirmed(confirmed);
		});
	}

	@Override
	public void onPaymentFailed(UUID orderId) {
		advance(orderId, "PaymentFailed", order -> {
			Order cancelled = order.cancel();
			repository.save(cancelled);
			commandPublisher.releaseStock(cancelled);
			eventPublisher.orderCancelled(cancelled);
		});
	}

	@Override
	public void onStockReleased(UUID orderId) {
		// Acknowledgement of the ReleaseStock compensation; the order is already CANCELLED. Observability only.
		log.info("Stock released (compensation acknowledged) for order {}", orderId);
	}

	private void advance(UUID orderId, String replyType, Consumer<Order> step) {
		Optional<Order> found = repository.findById(orderId);
		if (found.isEmpty()) {
			log.warn("Ignoring {} for unknown order {}", replyType, orderId);
			return;
		}
		try {
			step.accept(found.get());
		} catch (InvalidOrderStateException e) {
			log.warn("Ignoring out-of-order/duplicate {} for order {} in status {}", replyType, orderId,
					e.currentStatus());
		}
	}
}
