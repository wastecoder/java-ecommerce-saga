package com.wastecoder.shopflow.inventory.adapter.persistence;

import com.wastecoder.shopflow.inventory.adapter.persistence.database.StockItemJpaDatabase;
import com.wastecoder.shopflow.inventory.adapter.persistence.mapper.StockItemEntityMapper;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StockRepositoryImpl implements StockRepository {

	private final StockItemJpaDatabase database;
	private final StockItemEntityMapper mapper;

	public StockRepositoryImpl(StockItemJpaDatabase database, StockItemEntityMapper mapper) {
		this.database = database;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StockItem> findAll() {
		return database.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StockItem> findByProductId(UUID productId) {
		return database.findById(productId).map(mapper::toDomain);
	}

	@Override
	@Transactional
	public StockItem save(StockItem stockItem) {
		return mapper.toDomain(database.save(mapper.toEntity(stockItem)));
	}
}
