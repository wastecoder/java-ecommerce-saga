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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReserveStockUseCaseImplTest {

	@Mock
	private StockRepository stockRepository;

	@Mock
	private StockReservationRepository reservationRepository;

	@Mock
	private InventoryEventPublisher publisher;

	@InjectMocks
	private ReserveStockUseCaseImpl useCase;

	@Test
	@DisplayName("Given enough stock and no prior reservations, when ReserveStock, then stock is decremented, a RESERVED reservation is saved and StockReserved is published once")
	void reserve_happyPath_singleItem() {
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID))
				.thenReturn(Optional.of(StockItemMother.aStockItemFor(StockItemMother.PRODUCT_ID, 100)));

		useCase.execute(StockCommandMother.aValidCommand());

		ArgumentCaptor<StockItem> savedStock = ArgumentCaptor.forClass(StockItem.class);
		verify(stockRepository).save(savedStock.capture());
		assertThat(savedStock.getValue().available()).isEqualTo(98);
		assertThat(savedStock.getValue().reserved()).isEqualTo(2);

		ArgumentCaptor<StockReservation> savedReservation = ArgumentCaptor.forClass(StockReservation.class);
		verify(reservationRepository).save(savedReservation.capture());
		assertThat(savedReservation.getValue().status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(savedReservation.getValue().orderId()).isEqualTo(StockCommandMother.ORDER_ID);
		assertThat(savedReservation.getValue().productId()).isEqualTo(StockItemMother.PRODUCT_ID);
		assertThat(savedReservation.getValue().quantity()).isEqualTo(2);

		verify(publisher).stockReserved(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReservationFailed(any());
		verify(publisher, never()).stockReleased(any());
	}

	@Test
	@DisplayName("Given two items with enough stock, when ReserveStock, then both stocks are decremented and two reservations are saved before StockReserved")
	void reserve_happyPath_multiItem() {
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID))
				.thenReturn(Optional.of(StockItemMother.aStockItemFor(StockItemMother.PRODUCT_ID, 100)));
		when(stockRepository.findByProductId(StockItemMother.OTHER_PRODUCT_ID))
				.thenReturn(Optional.of(StockItemMother.aStockItemFor(StockItemMother.OTHER_PRODUCT_ID, 100)));

		useCase.execute(StockCommandMother.aTwoItemCommand());

		verify(stockRepository, times(2)).save(any());
		verify(reservationRepository, times(2)).save(any());
		verify(publisher).stockReserved(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReservationFailed(any());
	}

	@Test
	@DisplayName("Given availability exactly equal to the requested quantity, when ReserveStock, then it succeeds and StockReserved is published")
	void reserve_atBoundary_succeeds() {
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID))
				.thenReturn(Optional.of(StockItemMother.aStockItemFor(StockItemMother.PRODUCT_ID, 2)));

		useCase.execute(StockCommandMother.aValidCommand());

		ArgumentCaptor<StockItem> savedStock = ArgumentCaptor.forClass(StockItem.class);
		verify(stockRepository).save(savedStock.capture());
		assertThat(savedStock.getValue().available()).isZero();
		verify(publisher).stockReserved(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReservationFailed(any());
	}

	@Test
	@DisplayName("Given an item whose quantity exceeds availability, when ReserveStock, then no stock changes, a FAILED reservation is recorded and StockReservationFailed is published once")
	void reserve_insufficient_failsAndRecords() {
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID))
				.thenReturn(Optional.of(StockItemMother.aStockItemFor(StockItemMother.PRODUCT_ID, 1)));

		useCase.execute(StockCommandMother.aCommandRequesting(StockItemMother.PRODUCT_ID, 2));

		verify(stockRepository, never()).save(any());
		ArgumentCaptor<StockReservation> savedReservation = ArgumentCaptor.forClass(StockReservation.class);
		verify(reservationRepository).save(savedReservation.capture());
		assertThat(savedReservation.getValue().status()).isEqualTo(ReservationStatus.FAILED);
		verify(publisher).stockReservationFailed(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReserved(any());
	}

	@Test
	@DisplayName("Given no stock row exists for the product, when ReserveStock, then a FAILED reservation is recorded, no stock changes and StockReservationFailed is published")
	void reserve_missingProduct_fails() {
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID)).thenReturn(Optional.empty());

		useCase.execute(StockCommandMother.aCommandRequesting(StockItemMother.PRODUCT_ID, 2));

		verify(stockRepository, never()).save(any());
		verify(reservationRepository).save(any());
		verify(publisher).stockReservationFailed(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReserved(any());
	}

	@Test
	@DisplayName("Given the first item is available but the second is short, when ReserveStock, then no stock is decremented for either and StockReservationFailed is published")
	void reserve_allOrNothing_secondItemShort() {
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID))
				.thenReturn(Optional.of(StockItemMother.aStockItemFor(StockItemMother.PRODUCT_ID, 100)));
		when(stockRepository.findByProductId(StockItemMother.OTHER_PRODUCT_ID))
				.thenReturn(Optional.of(StockItemMother.aStockItemFor(StockItemMother.OTHER_PRODUCT_ID, 0)));

		useCase.execute(StockCommandMother.aTwoItemCommand());

		verify(stockRepository, never()).save(any());
		verify(reservationRepository, times(2)).save(any());
		verify(publisher).stockReservationFailed(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReserved(any());
	}

	@Test
	@DisplayName("Given reservations already RESERVED for the order, when ReserveStock is redelivered, then StockReserved is republished without new stock changes")
	void reserve_idempotencyGuard_reservedHit() {
		when(reservationRepository.findByOrderId(StockCommandMother.ORDER_ID))
				.thenReturn(StockReservationMother.aReservedListFor(StockCommandMother.ORDER_ID));

		useCase.execute(StockCommandMother.aValidCommand());

		verify(stockRepository, never()).save(any());
		verify(stockRepository, never()).findByProductId(any());
		verify(reservationRepository, never()).save(any());
		verify(publisher).stockReserved(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReservationFailed(any());
	}

	@Test
	@DisplayName("Given reservations already FAILED for the order, when ReserveStock is redelivered, then StockReservationFailed is republished without new writes")
	void reserve_idempotencyGuard_failedHit() {
		when(reservationRepository.findByOrderId(StockCommandMother.ORDER_ID))
				.thenReturn(List.of(StockReservationMother.aFailedReservation()));

		useCase.execute(StockCommandMother.aValidCommand());

		verify(stockRepository, never()).save(any());
		verify(reservationRepository, never()).save(any());
		verify(publisher).stockReservationFailed(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReserved(any());
	}

	@Test
	@DisplayName("Given only RELEASED reservations exist for the order, when ReserveStock is redelivered, then it reserves afresh (residual edge deferred to item 4 dedup)")
	void reserve_onlyReleasedReservations_reservesAfresh() {
		when(reservationRepository.findByOrderId(StockCommandMother.ORDER_ID))
				.thenReturn(List.of(StockReservationMother.aReleasedReservation()));
		when(stockRepository.findByProductId(StockItemMother.PRODUCT_ID))
				.thenReturn(Optional.of(StockItemMother.aStockItemFor(StockItemMother.PRODUCT_ID, 100)));

		useCase.execute(StockCommandMother.aValidCommand());

		verify(stockRepository).save(any());
		verify(reservationRepository).save(any());
		verify(publisher).stockReserved(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReservationFailed(any());
	}

	@Test
	@DisplayName("Given an empty items list, when ReserveStock, then StockReserved is published with no stock changes")
	void reserve_emptyItems_succeedsVacuously() {
		useCase.execute(StockCommandMother.anEmptyCommand());

		verify(stockRepository, never()).save(any());
		verify(reservationRepository, never()).save(any());
		verify(publisher).stockReserved(StockCommandMother.ORDER_ID);
		verify(publisher, never()).stockReservationFailed(any());
	}
}
