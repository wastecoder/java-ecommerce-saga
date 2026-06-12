package com.wastecoder.shopflow.inventory.adapter.persistence.mapper;

import com.wastecoder.shopflow.inventory.adapter.persistence.entity.StockItemEntity;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockItemEntityMapperTest {

	private final StockItemEntityMapper mapper = new StockItemEntityMapperImpl();

	@Test
	@DisplayName("Given null inputs, when mapping, then null is returned")
	void nullInputs_returnNull() {
		assertThat(mapper.toEntity(null)).isNull();
		assertThat(mapper.toDomain(null)).isNull();
	}

	@Test
	@DisplayName("Given a stock item, when mapped to entity and back, then all fields round-trip")
	void roundTrip_preservesFields() {
		StockItem domain = StockItemMother.aStockItem();

		StockItemEntity entity = mapper.toEntity(domain);
		assertThat(entity.getProductId()).isEqualTo(StockItemMother.PRODUCT_ID);
		assertThat(entity.getAvailable()).isEqualTo(100);
		assertThat(entity.getReserved()).isEqualTo(0);

		assertThat(mapper.toDomain(entity)).isEqualTo(domain);
	}
}
