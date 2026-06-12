package com.wastecoder.shopflow.order.application.usecase;

import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.domain.exception.OrderNotFoundException;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.testsupport.mother.OrderMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderUseCaseImplTest {

	@Mock
	private OrderRepository repository;

	@InjectMocks
	private GetOrderUseCaseImpl useCase;

	@Test
	@DisplayName("Given an existing order, when getting it by id, then the order is returned")
	void execute_returnsExistingOrder() {
		Order order = OrderMother.aPendingOrder();
		when(repository.findById(OrderMother.ORDER_ID)).thenReturn(Optional.of(order));

		Order result = useCase.execute(OrderMother.ORDER_ID);

		assertThat(result).isEqualTo(order);
	}

	@Test
	@DisplayName("Given no order with the given id, when getting it, then OrderNotFoundException is thrown")
	void execute_throwsWhenNotFound() {
		UUID missingId = UUID.fromString("99999999-9999-9999-9999-999999999999");
		when(repository.findById(missingId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.execute(missingId))
				.isInstanceOf(OrderNotFoundException.class)
				.extracting(ex -> ((OrderNotFoundException) ex).orderId())
				.isEqualTo(missingId);
	}
}
