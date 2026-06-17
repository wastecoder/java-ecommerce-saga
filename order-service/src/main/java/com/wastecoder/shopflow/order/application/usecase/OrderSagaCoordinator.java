package com.wastecoder.shopflow.order.application.usecase;

import com.wastecoder.shopflow.order.application.port.in.HandlePaymentReplyUseCase;
import com.wastecoder.shopflow.order.application.port.in.HandleStockReplyUseCase;
import com.wastecoder.shopflow.order.application.port.out.OrderCommandPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderEventPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.domain.exception.InvalidOrderStateException;
import com.wastecoder.shopflow.order.domain.model.Order;
import io.micrometer.core.instrument.MeterRegistry;
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
 * These domain state guards absorb a <em>re-issued</em> reply (a new {@code eventId}); a true redelivery
 * of the same {@code eventId} is short-circuited earlier by the {@code processed_messages} dedup in the
 * listener. The {@code catch} below stays narrow on purpose: only {@link InvalidOrderStateException} is an
 * idempotent no-op (its marker still commits); any other exception propagates, rolls back the marker and is
 * routed to the DLT. DB write and Kafka publish are still not atomic in the MVP (Transactional Outbox is
 * future work); operations are ordered save-then-publish.
 */
@Service
public class OrderSagaCoordinator implements HandleStockReplyUseCase, HandlePaymentReplyUseCase {

	private static final Logger log = LoggerFactory.getLogger(OrderSagaCoordinator.class);

	private final OrderRepository repository;
	private final OrderCommandPublisher commandPublisher;
	private final OrderEventPublisher eventPublisher;
	private final MeterRegistry meterRegistry;

	public OrderSagaCoordinator(OrderRepository repository, OrderCommandPublisher commandPublisher,
			OrderEventPublisher eventPublisher, MeterRegistry meterRegistry) {
		this.repository = repository;
		this.commandPublisher = commandPublisher;
		this.eventPublisher = eventPublisher;
		this.meterRegistry = meterRegistry;
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
			meterRegistry.counter("shopflow.saga.outcome", "outcome", "rejected").increment();
		});
	}

	@Override
	public void onPaymentAuthorized(UUID orderId) {
		advance(orderId, "PaymentAuthorized", order -> {
			Order confirmed = order.markPaid().confirm();
			repository.save(confirmed);
			eventPublisher.orderConfirmed(confirmed);
			meterRegistry.counter("shopflow.saga.outcome", "outcome", "confirmed").increment();
		});
	}

	@Override
	public void onPaymentFailed(UUID orderId) {
		advance(orderId, "PaymentFailed", order -> {
			Order cancelled = order.cancel();
			repository.save(cancelled);
			commandPublisher.releaseStock(cancelled);
			eventPublisher.orderCancelled(cancelled);
			meterRegistry.counter("shopflow.saga.outcome", "outcome", "cancelled").increment();
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
