package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.out.InventoryEventPublisher;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.application.port.out.StockReservationRepository;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockCommandMother;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockReservationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseStockUseCaseImplTest {

	@Mock
	private StockRepository stockRepository;

	@Mock
	private StockReservationRepository reservationRepository;

	@Mock
	private InventoryEventPublisher publisher;

	@InjectMocks
	private ReleaseStockUseCaseImpl useCase;

	@Test
	@DisplayName("Given a RESERVED reservation, when ReleaseStock, then stock is restored, the reservation is marked RELEASED and StockReleased is published once")
	void release_restoresStockAndReleasesReservation() {
		StockReservation reserved = StockReservation.reserve(
				StockReservationMother.RESERVATION_ID, StockCommandMother.ORDER_ID, StockItemMother.PRODUCT_ID, 2);
		when(reservationRepository.findByOrderIdAndStatus(StockCommandMother.ORDER_ID, ReservationStatus.RESERVED))
				.thenReturn(List.of(reserved));
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID))
				.thenReturn(Optional.of(new StockItem(StockItemMother.PRODUCT_ID, 98, 2)));

		useCase.execute(StockCommandMother.aValidCommand());

		ArgumentCaptor<StockItem> savedStock = ArgumentCaptor.forClass(StockItem.class);
		verify(stockRepository).save(savedStock.capture());
		assertThat(savedStock.getValue().available()).isEqualTo(100);
		assertThat(savedStock.getValue().reserved()).isZero();

		ArgumentCaptor<StockReservation> savedReservation = ArgumentCaptor.forClass(StockReservation.class);
		verify(reservationRepository).save(savedReservation.capture());
		assertThat(savedReservation.getValue().status()).isEqualTo(ReservationStatus.RELEASED);

		verify(publisher).stockReleased(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReserved(any());
		verify(publisher, never()).stockReservationFailed(any());
	}

	@Test
	@DisplayName("Given two RESERVED reservations, when ReleaseStock, then both stocks are restored and both reservations are released")
	void release_multipleReservations() {
		StockReservation r1 = StockReservation.reserve(
				StockReservationMother.RESERVATION_ID, StockCommandMother.ORDER_ID, StockItemMother.PRODUCT_ID, 2);
		StockReservation r2 = StockReservation.reserve(
				UUID.randomUUID(), StockCommandMother.ORDER_ID, StockItemMother.OTHER_PRODUCT_ID, 1);
		when(reservationRepository.findByOrderIdAndStatus(StockCommandMother.ORDER_ID, ReservationStatus.RESERVED))
				.thenReturn(List.of(r1, r2));
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID))
				.thenReturn(Optional.of(new StockItem(StockItemMother.PRODUCT_ID, 98, 2)));
		when(stockRepository.findByProductId(StockItemMother.OTHER_PRODUCT_ID))
				.thenReturn(Optional.of(new StockItem(StockItemMother.OTHER_PRODUCT_ID, 49, 1)));

		useCase.execute(StockCommandMother.aValidCommand());

		verify(stockRepository, times(2)).save(any());
		verify(reservationRepository, times(2)).save(any());
		verify(publisher).stockReleased(StockCommandMother.ORDER_ID);
	}

	@Test
	@DisplayName("Given no RESERVED reservations for the order, when ReleaseStock, then StockReleased is published as an idempotent ack and nothing is persisted")
	void release_noReservations_acksIdempotently() {
		when(reservationRepository.findByOrderIdAndStatus(StockCommandMother.ORDER_ID, ReservationStatus.RESERVED))
				.thenReturn(List.of());

		useCase.execute(StockCommandMother.aValidCommand());

		verify(stockRepository, never()).save(any());
		verify(reservationRepository, never()).save(any());
		verify(publisher).stockReleased(StockCommandMother.ORDER_ID);
	}
}
