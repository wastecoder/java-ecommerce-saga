package com.wastecoder.shopflow.inventory.application.port.out;

import com.wastecoder.shopflow.inventory.domain.model.StockItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository {

	List<StockItem> findAll();

	Optional<StockItem> findByProductId(UUID productId);

	StockItem save(StockItem stockItem);
}
