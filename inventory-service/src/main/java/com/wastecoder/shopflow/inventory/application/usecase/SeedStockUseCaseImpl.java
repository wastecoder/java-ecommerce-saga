package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.in.SeedStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.application.viewmodel.SeedResult;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeedStockUseCaseImpl implements SeedStockUseCase {

	private final StockRepository repository;

	public SeedStockUseCaseImpl(StockRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public SeedResult execute(List<StockItem> items) {
		int inserted = 0;
		int skipped = 0;
		for (StockItem item : items) {
			if (repository.findByProductId(item.productId()).isPresent()) {
				skipped++;
			} else {
				repository.save(item);
				inserted++;
			}
		}
		return new SeedResult(items.size(), inserted, skipped);
	}
}
