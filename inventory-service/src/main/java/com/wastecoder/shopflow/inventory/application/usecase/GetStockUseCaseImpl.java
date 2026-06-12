package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.in.GetStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.domain.exception.StockItemNotFoundException;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetStockUseCaseImpl implements GetStockUseCase {

	private final StockRepository repository;

	public GetStockUseCaseImpl(StockRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public StockItem execute(UUID productId) {
		return repository.findByProductId(productId)
				.orElseThrow(() -> new StockItemNotFoundException(productId));
	}
}
