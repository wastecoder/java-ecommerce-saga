package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.domain.exception.StockItemNotFoundException;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStockUseCaseImplTest {

	@Mock
	private StockRepository repository;

	@InjectMocks
	private GetStockUseCaseImpl useCase;

	@Test
	@DisplayName("Given a product with stock, when getting it, then the stock item is returned")
	void execute_returnsStock() {
		StockItem item = StockItemMother.aStockItem();
		when(repository.findByProductId(StockItemMother.PRODUCT_ID)).thenReturn(Optional.of(item));

		assertThat(useCase.execute(StockItemMother.PRODUCT_ID)).isEqualTo(item);
	}

	@Test
	@DisplayName("Given no stock for the product, when getting it, then StockItemNotFoundException is thrown")
	void execute_throwsWhenNotFound() {
		when(repository.findByProductId(StockItemMother.PRODUCT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.execute(StockItemMother.PRODUCT_ID))
				.isInstanceOf(StockItemNotFoundException.class)
				.extracting(ex -> ((StockItemNotFoundException) ex).productId())
				.isEqualTo(StockItemMother.PRODUCT_ID);
	}
}
