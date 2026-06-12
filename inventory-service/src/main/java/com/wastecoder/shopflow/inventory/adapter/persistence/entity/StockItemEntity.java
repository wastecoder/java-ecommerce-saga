package com.wastecoder.shopflow.inventory.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
public class StockItemEntity {

	@Id
	private UUID productId;

	@Column(nullable = false)
	private int available;

	@Column(nullable = false)
	private int reserved;
}
