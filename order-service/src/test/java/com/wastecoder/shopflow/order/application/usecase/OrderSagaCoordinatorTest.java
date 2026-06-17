package com.wastecoder.shopflow.order.application.usecase;

import com.wastecoder.shopflow.order.application.port.out.OrderCommandPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderEventPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderStatus;
import com.wastecoder.shopflow.order.testsupport.mother.OrderMother;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaCoordinatorTest {

	private static final java.util.UUID ORDER_ID = OrderMother.ORDER_ID;

	@Mock
	private OrderRepository repository;

	@Mock
	private OrderCommandPublisher commandPublisher;

	@Mock
	private OrderEventPublisher eventPublisher;

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private OrderSagaCoordinator coordinator;

	@BeforeEach
	void setUp() {
		coordinator = new OrderSagaCoordinator(repository, commandPublisher, eventPublisher, meterRegistry);
	}

	private ArgumentCaptor<Order> savedOrder() {
		return ArgumentCaptor.forClass(Order.class);
	}

	// --- Happy paths ---------------------------------------------------------

	@Test
	@DisplayName("Given a PENDING order, when StockReserved, then it is saved STOCK_RESERVED and ProcessPayment is sent")
	void onStockReserved_advancesAndRequestsPayment() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aPendingOrder()));

		coordinator.onStockReserved(ORDER_ID);

		ArgumentCaptor<Order> captor = savedOrder();
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(OrderStatus.STOCK_RESERVED);
		verify(commandPublisher).processPayment(captor.getValue());
		verify(commandPublisher, never()).reserveStock(any());
		verify(commandPublisher, never()).releaseStock(any());
		verifyNoInteractions(eventPublisher);

		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given a STOCK_RESERVED order, when PaymentAuthorized, then it is saved CONFIRMED (not PAID) and OrderConfirmed is emitted")
	void onPaymentAuthorized_confirms() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aStockReservedOrder()));

		coordinator.onPaymentAuthorized(ORDER_ID);

		ArgumentCaptor<Order> captor = savedOrder();
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(OrderStatus.CONFIRMED);
		verify(eventPublisher).orderConfirmed(captor.getValue());
		verify(eventPublisher, never()).orderRejected(any());
		verify(eventPublisher, never()).orderCancelled(any());
		verifyNoInteractions(commandPublisher);

		assertThat(meterRegistry.get("shopflow.saga.outcome").tag("outcome", "confirmed").counter().count()).isEqualTo(1.0);
		assertThat(meterRegistry.find("shopflow.saga.outcome").tag("outcome", "rejected").counter()).isNull();
		assertThat(meterRegistry.find("shopflow.saga.outcome").tag("outcome", "cancelled").counter()).isNull();
	}

	@Test
	@DisplayName("Given a PENDING order, when StockReservationFailed, then it is saved REJECTED and OrderRejected is emitted (no compensation)")
	void onStockReservationFailed_rejects() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aPendingOrder()));

		coordinator.onStockReservationFailed(ORDER_ID);

		ArgumentCaptor<Order> captor = savedOrder();
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(OrderStatus.REJECTED);
		verify(eventPublisher).orderRejected(captor.getValue());
		verify(eventPublisher, never()).orderCancelled(any());
		verify(eventPublisher, never()).orderConfirmed(any());
		verifyNoInteractions(commandPublisher);

		assertThat(meterRegistry.get("shopflow.saga.outcome").tag("outcome", "rejected").counter().count()).isEqualTo(1.0);
		assertThat(meterRegistry.find("shopflow.saga.outcome").tag("outcome", "confirmed").counter()).isNull();
		assertThat(meterRegistry.find("shopflow.saga.outcome").tag("outcome", "cancelled").counter()).isNull();
	}

	@Test
	@DisplayName("Given a STOCK_RESERVED order, when PaymentFailed, then it is saved CANCELLED, ReleaseStock compensates and OrderCancelled is emitted")
	void onPaymentFailed_cancelsAndCompensates() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aStockReservedOrder()));

		coordinator.onPaymentFailed(ORDER_ID);

		ArgumentCaptor<Order> captor = savedOrder();
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(OrderStatus.CANCELLED);
		verify(commandPublisher).releaseStock(captor.getValue());
		verify(eventPublisher).orderCancelled(captor.getValue());
		verify(commandPublisher, never()).reserveStock(any());
		verify(commandPublisher, never()).processPayment(any());
		verify(eventPublisher, never()).orderConfirmed(any());
		verify(eventPublisher, never()).orderRejected(any());

		assertThat(meterRegistry.get("shopflow.saga.outcome").tag("outcome", "cancelled").counter().count()).isEqualTo(1.0);
		assertThat(meterRegistry.find("shopflow.saga.outcome").tag("outcome", "confirmed").counter()).isNull();
		assertThat(meterRegistry.find("shopflow.saga.outcome").tag("outcome", "rejected").counter()).isNull();
	}

	@Test
	@DisplayName("Given any order, when StockReleased (compensation ack), then nothing changes (no save, no publish)")
	void onStockReleased_isNoOp() {
		coordinator.onStockReleased(ORDER_ID);

		verifyNoInteractions(repository, commandPublisher, eventPublisher);
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	// --- Guards: duplicate / out-of-order / terminal-state replies are ignored ----

	@Test
	@DisplayName("Given an already STOCK_RESERVED order, when a duplicate StockReserved arrives, then it is ignored")
	void onStockReserved_duplicate_isIgnored() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aStockReservedOrder()));

		assertThatNoException().isThrownBy(() -> coordinator.onStockReserved(ORDER_ID));

		verify(repository, never()).save(any());
		verifyNoInteractions(commandPublisher, eventPublisher);
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given a PENDING order, when PaymentAuthorized arrives out of order, then it is ignored")
	void onPaymentAuthorized_outOfOrder_isIgnored() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aPendingOrder()));

		assertThatNoException().isThrownBy(() -> coordinator.onPaymentAuthorized(ORDER_ID));

		verify(repository, never()).save(any());
		verifyNoInteractions(commandPublisher, eventPublisher);
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given a CANCELLED order, when PaymentAuthorized arrives, then the terminal order is not changed")
	void onPaymentAuthorized_onTerminal_isIgnored() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aCancelledOrder()));

		assertThatNoException().isThrownBy(() -> coordinator.onPaymentAuthorized(ORDER_ID));

		verify(repository, never()).save(any());
		verifyNoInteractions(commandPublisher, eventPublisher);
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given a CONFIRMED order, when PaymentFailed arrives, then the terminal order is not changed")
	void onPaymentFailed_onTerminal_isIgnored() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aConfirmedOrder()));

		assertThatNoException().isThrownBy(() -> coordinator.onPaymentFailed(ORDER_ID));

		verify(repository, never()).save(any());
		verifyNoInteractions(commandPublisher, eventPublisher);
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given a STOCK_RESERVED order, when StockReservationFailed arrives out of order, then it is ignored")
	void onStockReservationFailed_outOfOrder_isIgnored() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aStockReservedOrder()));

		assertThatNoException().isThrownBy(() -> coordinator.onStockReservationFailed(ORDER_ID));

		verify(repository, never()).save(any());
		verifyNoInteractions(commandPublisher, eventPublisher);
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	// --- Unknown order: replies for a missing order are dropped, not thrown ----

	@Test
	@DisplayName("Given no such order, when StockReserved arrives, then it is dropped without saving or publishing")
	void onStockReserved_unknownOrder_isDropped() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());

		assertThatNoException().isThrownBy(() -> coordinator.onStockReserved(ORDER_ID));

		verify(repository, never()).save(any());
		verifyNoInteractions(commandPublisher, eventPublisher);
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given no such order, when PaymentFailed arrives, then it is dropped without saving or publishing")
	void onPaymentFailed_unknownOrder_isDropped() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.empty());

		assertThatNoException().isThrownBy(() -> coordinator.onPaymentFailed(ORDER_ID));

		verify(repository, never()).save(any());
		verifyNoInteractions(commandPublisher, eventPublisher);
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}

	// --- Partial failure: a publish error propagates (Kafka redelivers) ----

	@Test
	@DisplayName("Given the next command fails to publish, when handling StockReserved, then the exception propagates after the state was saved")
	void onStockReserved_publishFailure_propagates() {
		when(repository.findById(ORDER_ID)).thenReturn(Optional.of(OrderMother.aPendingOrder()));
		doThrow(new RuntimeException("broker unavailable")).when(commandPublisher).processPayment(any());

		assertThatThrownBy(() -> coordinator.onStockReserved(ORDER_ID))
				.isInstanceOf(RuntimeException.class);

		verify(repository).save(any());
		assertThat(meterRegistry.find("shopflow.saga.outcome").counter()).isNull();
	}
}
