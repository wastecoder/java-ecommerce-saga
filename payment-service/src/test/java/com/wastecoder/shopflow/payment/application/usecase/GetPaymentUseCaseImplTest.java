package com.wastecoder.shopflow.payment.application.usecase;

import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.domain.exception.PaymentNotFoundException;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.testsupport.mother.PaymentMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPaymentUseCaseImplTest {

	@Mock
	private PaymentRepository repository;

	@InjectMocks
	private GetPaymentUseCaseImpl useCase;

	@Test
	@DisplayName("Given a payment for the order, when getting it, then the payment is returned")
	void execute_returnsPayment() {
		Payment payment = PaymentMother.anAuthorizedPayment();
		when(repository.findByOrderId(PaymentMother.ORDER_ID)).thenReturn(Optional.of(payment));

		assertThat(useCase.execute(PaymentMother.ORDER_ID)).isEqualTo(payment);
	}

	@Test
	@DisplayName("Given no payment for the order, when getting it, then PaymentNotFoundException is thrown")
	void execute_throwsWhenNotFound() {
		when(repository.findByOrderId(PaymentMother.ORDER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.execute(PaymentMother.ORDER_ID))
				.isInstanceOf(PaymentNotFoundException.class)
				.extracting(ex -> ((PaymentNotFoundException) ex).orderId())
				.isEqualTo(PaymentMother.ORDER_ID);
	}
}
