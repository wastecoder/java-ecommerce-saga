package com.wastecoder.shopflow.inventory.adapter.persistence.entity;

import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
public class StockReservationEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID orderId;

	@Column(nullable = false)
	private UUID productId;

	@Column(nullable = false)
	private int quantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status;
}
