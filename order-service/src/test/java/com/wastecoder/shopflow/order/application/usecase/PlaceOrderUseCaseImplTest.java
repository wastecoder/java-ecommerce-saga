package com.wastecoder.shopflow.order.application.usecase;

import com.wastecoder.shopflow.order.application.port.out.OrderCommandPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderEventPublisher;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderStatus;
import com.wastecoder.shopflow.order.testsupport.mother.PlaceOrderCommandMother;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceOrderUseCaseImplTest {

	@Mock
	private OrderRepository repository;

	@Mock
	private OrderEventPublisher eventPublisher;

	@Mock
	private OrderCommandPublisher commandPublisher;

	@Mock
	private Validator validator;

	@InjectMocks
	private PlaceOrderUseCaseImpl useCase;

	@Test
	@DisplayName("Given a valid command, when placing an order, then it is saved as PENDING with generated ids and computed total")
	void execute_placesPendingOrder() {
		when(validator.validate(any(Order.class))).thenReturn(Collections.emptySet());
		when(repository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Order result = useCase.execute(PlaceOrderCommandMother.aValidCommand());

		ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
		verify(repository).save(captor.capture());
		Order saved = captor.getValue();

		assertThat(result).isEqualTo(saved);
		assertThat(saved.status()).isEqualTo(OrderStatus.PENDING);
		assertThat(saved.totalAmount()).isEqualByComparingTo(new BigDecimal("21.00"));
		assertThat(saved.id()).isNotNull();
		assertThat(saved.createdAt()).isNotNull();
		assertThat(saved.items()).singleElement().satisfies(item -> {
			assertThat(item.id()).isNotNull();
			assertThat(item.productId()).isEqualTo(PlaceOrderCommandMother.aValidCommand().items().get(0).productId());
			assertThat(item.quantity()).isEqualTo(2);
			assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("10.50"));
		});

		verify(eventPublisher).orderCreated(result);
		verify(commandPublisher).reserveStock(result);
	}

	@Test
	@DisplayName("Given a command that produces an invalid order, when placing it, then it throws and nothing is saved")
	void execute_rejectsInvalidOrder() {
		@SuppressWarnings("unchecked")
		ConstraintViolation<Order> violation = (ConstraintViolation<Order>) org.mockito.Mockito.mock(ConstraintViolation.class);
		Set<ConstraintViolation<Order>> violations = Set.of(violation);
		when(validator.validate(any(Order.class))).thenReturn(violations);

		assertThatThrownBy(() -> useCase.execute(PlaceOrderCommandMother.aValidCommand()))
				.isInstanceOf(ConstraintViolationException.class);

		verify(repository, never()).save(any(Order.class));
		verify(eventPublisher, never()).orderCreated(any());
		verify(commandPublisher, never()).reserveStock(any());
	}
}
