package com.wastecoder.shopflow.inventory.adapter.persistence.database;

import com.wastecoder.shopflow.inventory.adapter.persistence.entity.StockReservationEntity;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockReservationJpaDatabase extends JpaRepository<StockReservationEntity, UUID> {

	List<StockReservationEntity> findByOrderId(UUID orderId);

	List<StockReservationEntity> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
