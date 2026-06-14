package com.wastecoder.shopflow.inventory.adapter.persistence;

import com.wastecoder.shopflow.inventory.adapter.persistence.database.StockReservationJpaDatabase;
import com.wastecoder.shopflow.inventory.adapter.persistence.mapper.StockReservationEntityMapper;
import com.wastecoder.shopflow.inventory.application.port.out.StockReservationRepository;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class StockReservationRepositoryImpl implements StockReservationRepository {

	private final StockReservationJpaDatabase database;
	private final StockReservationEntityMapper mapper;

	public StockReservationRepositoryImpl(StockReservationJpaDatabase database,
			StockReservationEntityMapper mapper) {
		this.database = database;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public StockReservation save(StockReservation reservation) {
		return mapper.toDomain(database.save(mapper.toEntity(reservation)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StockReservation> findByOrderId(UUID orderId) {
		return database.findByOrderId(orderId).stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status) {
		return database.findByOrderIdAndStatus(orderId, status).stream().map(mapper::toDomain).toList();
	}
}
