package com.wastecoder.shopflow.inventory.application.port.out;

import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;

import java.util.List;
import java.util.UUID;

public interface StockReservationRepository {

	StockReservation save(StockReservation reservation);

	List<StockReservation> findByOrderId(UUID orderId);

	List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
