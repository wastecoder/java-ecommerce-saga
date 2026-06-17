package com.wastecoder.shopflow.order.application.usecase;

import com.wastecoder.shopflow.order.application.port.in.PlaceOrderUseCase;
import com.wastecoder.shopflow.order.application.port.out.OrderCommandPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderEventPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.application.viewmodel.PlaceOrderCommand;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderItem;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {

	private final OrderRepository repository;
	private final OrderEventPublisher eventPublisher;
	private final OrderCommandPublisher commandPublisher;
	private final Validator validator;
	private final MeterRegistry meterRegistry;

	public PlaceOrderUseCaseImpl(OrderRepository repository, OrderEventPublisher eventPublisher,
			OrderCommandPublisher commandPublisher, Validator validator, MeterRegistry meterRegistry) {
		this.repository = repository;
		this.eventPublisher = eventPublisher;
		this.commandPublisher = commandPublisher;
		this.validator = validator;
		this.meterRegistry = meterRegistry;
	}

	@Override
	@Transactional
	public Order execute(PlaceOrderCommand command) {
		List<OrderItem> items = command.items().stream()
				.map(item -> new OrderItem(UUID.randomUUID(), item.productId(), item.quantity(), item.unitPrice()))
				.toList();
		Order order = Order.place(UUID.randomUUID(), command.customerId(), items, Instant.now());
		ensureValid(order);
		Order saved = repository.save(order);
		// Start the saga: announce the order, then ask inventory to reserve stock.
		eventPublisher.orderCreated(saved);
		commandPublisher.reserveStock(saved);
		meterRegistry.counter("shopflow.orders.placed").increment();
		return saved;
	}

	private void ensureValid(Order order) {
		Set<ConstraintViolation<Order>> violations = validator.validate(order);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
