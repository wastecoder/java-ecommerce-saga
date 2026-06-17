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
class AuthorizePaymentUseCaseImplTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentGateway paymentGateway;

	@Mock
	private PaymentEventPublisher publisher;

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private AuthorizePaymentUseCaseImpl useCase;

	@BeforeEach
	void setUp() {
		useCase = new AuthorizePaymentUseCaseImpl(paymentRepository, paymentGateway, publisher, meterRegistry);
	}

	@Test
	@DisplayName("Given no prior payment and the PSP approves, when ProcessPayment, then an AUTHORIZED payment is saved and PaymentAuthorized is published once")
	void authorize_approved() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID)).thenReturn(Optional.empty());
		when(paymentGateway.authorize(any(), any())).thenReturn(PaymentDecision.approved("PSP-ref-1"));

		useCase.execute(PaymentCommandMother.aProcessCommand());

		ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(saved.capture());
		assertThat(saved.getValue().status()).isEqualTo(PaymentStatus.AUTHORIZED);
		assertThat(saved.getValue().orderId()).isEqualTo(PaymentCommandMother.ORDER_ID);
		assertThat(saved.getValue().providerRef()).isEqualTo("PSP-ref-1");
		assertThat(saved.getValue().amount()).isEqualByComparingTo(PaymentMother.AMOUNT);

		verify(publisher).paymentAuthorized(PaymentCommandMother.ORDER_ID);
		verify(publisher, never()).paymentFailed(any());
		verify(publisher, never()).paymentRefunded(any());

		assertThat(meterRegistry.get("shopflow.payments.outcome").tag("outcome", "authorized").counter().count()).isEqualTo(1.0);
		assertThat(meterRegistry.find("shopflow.payments.outcome").tag("outcome", "failed").counter()).isNull();
	}

	@Test
	@DisplayName("Given no prior payment and the PSP declines, when ProcessPayment, then a FAILED payment is saved and PaymentFailed is published once")
	void authorize_declined() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID)).thenReturn(Optional.empty());
		when(paymentGateway.authorize(any(), any())).thenReturn(PaymentDecision.declined("PSP-ref-2", "insufficient funds"));

		useCase.execute(PaymentCommandMother.aProcessCommand());

		ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(saved.capture());
		assertThat(saved.getValue().status()).isEqualTo(PaymentStatus.FAILED);
		assertThat(saved.getValue().providerRef()).isEqualTo("PSP-ref-2");

		verify(publisher).paymentFailed(PaymentCommandMother.ORDER_ID);
		verify(publisher, never()).paymentAuthorized(any());

		assertThat(meterRegistry.get("shopflow.payments.outcome").tag("outcome", "failed").counter().count()).isEqualTo(1.0);
		assertThat(meterRegistry.find("shopflow.payments.outcome").tag("outcome", "authorized").counter()).isNull();
	}

	@Test
	@DisplayName("Given an already AUTHORIZED payment, when ProcessPayment is redelivered, then PaymentAuthorized is republished without charging again")
	void authorize_idempotent_authorizedHit() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID))
				.thenReturn(Optional.of(PaymentMother.anAuthorizedPayment()));

		useCase.execute(PaymentCommandMother.aProcessCommand());

		verifyNoInteractions(paymentGateway);
		verify(paymentRepository, never()).save(any());
		verify(publisher).paymentAuthorized(PaymentCommandMother.ORDER_ID);
		verify(publisher, never()).paymentFailed(any());

		assertThat(meterRegistry.find("shopflow.payments.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given an already FAILED payment, when ProcessPayment is redelivered, then PaymentFailed is republished without charging again")
	void authorize_idempotent_failedHit() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID))
				.thenReturn(Optional.of(PaymentMother.aFailedPayment()));

		useCase.execute(PaymentCommandMother.aProcessCommand());

		verifyNoInteractions(paymentGateway);
		verify(paymentRepository, never()).save(any());
		verify(publisher).paymentFailed(PaymentCommandMother.ORDER_ID);
		verify(publisher, never()).paymentAuthorized(any());

		assertThat(meterRegistry.find("shopflow.payments.outcome").counter()).isNull();
	}

	@Test
	@DisplayName("Given an already REFUNDED payment, when ProcessPayment is redelivered, then it is ignored with no charge and no reply")
	void authorize_idempotent_refundedHit() {
		when(paymentRepository.findByOrderId(PaymentCommandMother.ORDER_ID))
				.thenReturn(Optional.of(PaymentMother.aRefundedPayment()));

		useCase.execute(PaymentCommandMother.aProcessCommand());

		verifyNoInteractions(paymentGateway, publisher);
		verify(paymentRepository, never()).save(any());

		assertThat(meterRegistry.find("shopflow.payments.outcome").counter()).isNull();
	}
}
