package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.application.viewmodel.SeedResult;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedStockUseCaseImplTest {

	@Mock
	private StockRepository repository;

	@InjectMocks
	private SeedStockUseCaseImpl useCase;

	@Test
	@DisplayName("Given a mix of new and existing products, when seeding, then only absent ones are inserted")
	void execute_isIdempotent() {
		StockItem fresh = StockItemMother.aStockItemFor(StockItemMother.PRODUCT_ID, 100);
		StockItem existing = StockItemMother.aStockItemFor(StockItemMother.OTHER_PRODUCT_ID, 50);
		when(repository.findByProductId(StockItemMother.PRODUCT_ID)).thenReturn(Optional.empty());
		when(repository.findByProductId(StockItemMother.OTHER_PRODUCT_ID)).thenReturn(Optional.of(existing));

		SeedResult result = useCase.execute(List.of(fresh, existing));

		assertThat(result).isEqualTo(new SeedResult(2, 1, 1));
		verify(repository, times(1)).save(fresh);
		verify(repository, never()).save(existing);
	}

	@Test
	@DisplayName("Given an empty seed list, when seeding, then nothing is inserted")
	void execute_withEmptyList() {
		SeedResult result = useCase.execute(List.of());

		assertThat(result).isEqualTo(new SeedResult(0, 0, 0));
		verify(repository, never()).save(any());
	}
}
