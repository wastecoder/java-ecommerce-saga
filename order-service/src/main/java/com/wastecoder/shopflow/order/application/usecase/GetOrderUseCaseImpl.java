package com.wastecoder.shopflow.order.application.usecase;

import com.wastecoder.shopflow.order.application.port.in.GetOrderUseCase;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.domain.exception.OrderNotFoundException;
import com.wastecoder.shopflow.order.domain.model.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetOrderUseCaseImpl implements GetOrderUseCase {

	private final OrderRepository repository;

	public GetOrderUseCaseImpl(OrderRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Order execute(UUID orderId) {
		return repository.findById(orderId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));
	}
}
