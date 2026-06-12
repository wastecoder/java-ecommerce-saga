package com.wastecoder.shopflow.order.application.port.out;

import com.wastecoder.shopflow.order.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

	Order save(Order order);

	Optional<Order> findById(UUID id);
}
