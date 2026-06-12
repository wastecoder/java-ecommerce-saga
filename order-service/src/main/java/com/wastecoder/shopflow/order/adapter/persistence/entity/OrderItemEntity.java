package com.wastecoder.shopflow.order.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemEntity {

	@Id
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private OrderEntity order;

	@Column(nullable = false)
	private UUID productId;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;
}
