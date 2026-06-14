package com.wastecoder.shopflow.inventory.adapter.persistence;

import com.wastecoder.shopflow.inventory.TestcontainersConfiguration;
import com.wastecoder.shopflow.inventory.application.port.out.StockReservationRepository;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StockReservationRepositoryImplIntegrationTest {

	@Autowired
	private StockReservationRepository repository;

	@Test
	@DisplayName("Given a reservation, when saved and read back, then it is found by order id")
	void saveAndFindByOrderId_roundTrips() {
		UUID orderId = UUID.randomUUID();
		StockReservation reservation = StockReservation.reserve(UUID.randomUUID(), orderId, UUID.randomUUID(), 3);

		repository.save(reservation);

		List<StockReservation> found = repository.findByOrderId(orderId);
		assertThat(found).containsExactly(reservation);
	}

	@Test
	@DisplayName("Given RESERVED and RELEASED reservations for an order, when querying by RESERVED status, then only the RESERVED one is returned")
	void findByOrderIdAndStatus_filtersByStatus() {
		UUID orderId = UUID.randomUUID();
		StockReservation reserved = StockReservation.reserve(UUID.randomUUID(), orderId, UUID.randomUUID(), 1);
		StockReservation released = StockReservation.reserve(UUID.randomUUID(), orderId, UUID.randomUUID(), 2).release();
		repository.save(reserved);
		repository.save(released);

		List<StockReservation> found = repository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

		assertThat(found).containsExactly(reserved);
	}
}
