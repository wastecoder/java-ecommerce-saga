package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.in.ListStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListStockUseCaseImpl implements ListStockUseCase {

	private final StockRepository repository;

	public ListStockUseCaseImpl(StockRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StockItem> execute() {
		return repository.findAll();
	}
}
