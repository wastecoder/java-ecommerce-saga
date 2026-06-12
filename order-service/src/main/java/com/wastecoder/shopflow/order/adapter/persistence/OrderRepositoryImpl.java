package com.wastecoder.shopflow.order.adapter.persistence;

import com.wastecoder.shopflow.order.adapter.persistence.database.OrderJpaDatabase;
import com.wastecoder.shopflow.order.adapter.persistence.entity.OrderEntity;
import com.wastecoder.shopflow.order.adapter.persistence.mapper.OrderEntityMapper;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.domain.model.Order;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

	private final OrderJpaDatabase database;
	private final OrderEntityMapper mapper;

	public OrderRepositoryImpl(OrderJpaDatabase database, OrderEntityMapper mapper) {
		this.database = database;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public Order save(Order order) {
		OrderEntity saved = database.save(mapper.toEntity(order));
		return mapper.toDomain(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Order> findById(UUID id) {
		// Runs inside a transaction so the lazy items collection is initialised while mapping to domain.
		return database.findById(id).map(mapper::toDomain);
	}
}
