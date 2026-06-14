package com.wastecoder.shopflow.inventory.adapter.persistence.mapper;

import com.wastecoder.shopflow.inventory.adapter.persistence.entity.StockReservationEntity;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockReservationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockReservationEntityMapperTest {

	private final StockReservationEntityMapper mapper = new StockReservationEntityMapperImpl();

	@Test
	@DisplayName("Given null inputs, when mapping, then null is returned")
	void nullInputs_returnNull() {
		assertThat(mapper.toEntity(null)).isNull();
		assertThat(mapper.toDomain(null)).isNull();
	}

	@Test
	@DisplayName("Given a RESERVED reservation, when mapped to entity and back, then all fields round-trip")
	void roundTrip_reserved() {
		StockReservation domain = StockReservationMother.aReservedReservation();

		StockReservationEntity entity = mapper.toEntity(domain);
		assertThat(entity.getId()).isEqualTo(domain.id());
		assertThat(entity.getOrderId()).isEqualTo(domain.orderId());
		assertThat(entity.getProductId()).isEqualTo(domain.productId());
		assertThat(entity.getQuantity()).isEqualTo(domain.quantity());
		assertThat(entity.getStatus()).isEqualTo(ReservationStatus.RESERVED);

		assertThat(mapper.toDomain(entity)).isEqualTo(domain);
	}

	@Test
	@DisplayName("Given a FAILED reservation, when mapped to entity and back, then the status round-trips")
	void roundTrip_failed() {
		StockReservation domain = StockReservationMother.aFailedReservation();

		assertThat(mapper.toDomain(mapper.toEntity(domain))).isEqualTo(domain);
	}
}
