package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListStockUseCaseImplTest {

	@Mock
	private StockRepository repository;

	@InjectMocks
	private ListStockUseCaseImpl useCase;

	@Test
	@DisplayName("Given stock items exist, when listing stock, then all items are returned")
	void execute_returnsAllStock() {
		List<StockItem> stock = List.of(StockItemMother.aStockItem());
		when(repository.findAll()).thenReturn(stock);

		assertThat(useCase.execute()).isEqualTo(stock);
	}
}
