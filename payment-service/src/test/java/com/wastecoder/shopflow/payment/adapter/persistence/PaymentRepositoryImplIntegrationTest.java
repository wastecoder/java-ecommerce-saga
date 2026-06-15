package com.wastecoder.shopflow.payment.adapter.persistence;

import com.wastecoder.shopflow.payment.TestcontainersConfiguration;
import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaymentRepositoryImplIntegrationTest {

	@Autowired
	private PaymentRepository repository;

	@Test
	@DisplayName("Given a payment, when saved and read back, then it is found by order id with all fields preserved")
	void saveAndFindByOrderId_roundTrips() {
		UUID orderId = UUID.randomUUID();
		Payment payment = Payment.authorized(UUID.randomUUID(), orderId, new BigDecimal("100.00"), "PSP-ref-xyz");

		repository.save(payment);

		Payment found = repository.findByOrderId(orderId).orElseThrow();
		assertThat(found.id()).isEqualTo(payment.id());
		assertThat(found.orderId()).isEqualTo(orderId);
		assertThat(found.status()).isEqualTo(PaymentStatus.AUTHORIZED);
		assertThat(found.amount()).isEqualByComparingTo("100.00");
		assertThat(found.providerRef()).isEqualTo("PSP-ref-xyz");
	}

	@Test
	@DisplayName("Given no payment for an order, when querying by order id, then the result is empty")
	void findByOrderId_emptyWhenAbsent() {
		Optional<Payment> found = repository.findByOrderId(UUID.randomUUID());

		assertThat(found).isEmpty();
	}
}
