package com.wastecoder.shopflow.payment.application.usecase;

import com.wastecoder.shopflow.payment.application.port.out.PaymentDecision;
import com.wastecoder.shopflow.payment.application.port.out.PaymentEventPublisher;
import com.wastecoder.shopflow.payment.application.port.out.PaymentGateway;
import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;
import com.wastecoder.shopflow.payment.testsupport.mother.PaymentCommandMother;
import com.wastecoder.shopflow.payment.testsupport.mother.PaymentMother;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundPaymentUseCaseImplTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentGateway paymentGateway;

	@Mock
	private PaymentEventPublisher publisher;

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private RefundPaymentUseCaseImpl useCase;

	@BeforeEach
	void setUp() {
		useCase = new RefundPaymentUseCaseImpl(paymentRepository, paymentGateway, publisher, meterRegistry);
	}

	@Test
	@DisplayName("Given an AUTHORIZED payment, when RefundPayment, then it is refunded at the PSP, marked REFUNDED and PaymentRefunded is published once")
	void refund_authorized() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID))
				.thenReturn(Optional.of(PaymentMother.anAuthorizedPayment()));
		when(paymentGateway.refund(any(), any(), any())).thenReturn(PaymentDecision.approved("PSP-REFUND-1"));

		useCase.execute(PaymentCommandMother.aRefundCommand());

		ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(saved.capture());
		assertThat(saved.getValue().status()).isEqualTo(PaymentStatus.REFUNDED);
		verify(publisher).paymentRefunded(PaymentCommandMother.ORDER_ID);

		assertThat(meterRegistry.get("shopflow.payments.outcome").tag("outcome", "refunded").counter().count()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Given an already REFUNDED payment, when RefundPayment is redelivered, then PaymentRefunded is republished without refunding again")
	void refund_idempotent_refundedHit() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID))
				.thenReturn(Optional.of(PaymentMother.aRefundedPayment()));

		useCase.execute(PaymentCommandMother.aRefundCommand());

		verifyNoInteractions(paymentGateway);
		verify(paymentRepository, never()).save(any());
		verify(publisher).paymentRefunded(PaymentCommandMother.ORDER_ID);

		assertThat(meterRegistry.find("shopflow.payments.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given no payment for the order, when RefundPayment, then it is a no-op with nothing refunded or published")
	void refund_unknownPayment_noOp() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID)).thenReturn(Optional.empty());

		useCase.execute(PaymentCommandMother.aRefundCommand());

		verifyNoInteractions(paymentGateway, publisher);
		verify(paymentRepository, never()).save(any());

		assertThat(meterRegistry.find("shopflow.payments.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given a FAILED payment, when RefundPayment, then it is a no-op (there is nothing to refund)")
	void refund_failedPayment_noOp() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID))
				.thenReturn(Optional.of(PaymentMother.aFailedPayment()));

		useCase.execute(PaymentCommandMother.aRefundCommand());

		verifyNoInteractions(paymentGateway, publisher);
		verify(paymentRepository, never()).save(any());

		assertThat(meterRegistry.find("shopflow.payments.outcome").counter()).isNull();
	}
}
