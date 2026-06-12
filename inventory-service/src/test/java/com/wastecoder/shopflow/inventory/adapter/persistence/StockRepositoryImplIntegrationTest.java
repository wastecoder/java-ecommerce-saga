package com.wastecoder.shopflow.inventory.adapter.persistence;

import com.wastecoder.shopflow.inventory.TestcontainersConfiguration;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StockRepositoryImplIntegrationTest {

	@Autowired
	private StockRepository repository;

	@Test
	@DisplayName("Given a stock item, when saved and read back, then it is found by product id and listed")
	void saveAndFind_roundTrips() {
		StockItem item = StockItemMother.aStockItemFor(StockItemMother.OTHER_PRODUCT_ID, 42);

		repository.save(item);

		Optional<StockItem> found = repository.findByProductId(StockItemMother.OTHER_PRODUCT_ID);
		assertThat(found).contains(item);
		assertThat(repository.findAll()).contains(item);
	}

	@Test
	@DisplayName("Given no stock for a product, when findByProductId is called, then an empty optional is returned")
	void findByProductId_returnsEmptyWhenMissing() {
		Optional<StockItem> found = repository.findByProductId(StockItemMother.PRODUCT_ID);

		assertThat(found).isEmpty();
	}
}
