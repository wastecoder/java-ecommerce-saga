package com.wastecoder.shopflow.order.application.usecase;

import com.wastecoder.shopflow.order.application.port.in.PlaceOrderUseCase;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.application.viewmodel.PlaceOrderCommand;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderItem;
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
	private final Validator validator;

	public PlaceOrderUseCaseImpl(OrderRepository repository, Validator validator) {
		this.repository = repository;
		this.validator = validator;
	}

	@Override
	@Transactional
	public Order execute(PlaceOrderCommand command) {
		List<OrderItem> items = command.items().stream()
				.map(item -> new OrderItem(UUID.randomUUID(), item.productId(), item.quantity(), item.unitPrice()))
				.toList();
		Order order = Order.place(UUID.randomUUID(), command.customerId(), items, Instant.now());
		ensureValid(order);
		return repository.save(order);
	}

	private void ensureValid(Order order) {
		Set<ConstraintViolation<Order>> violations = validator.validate(order);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}
}
