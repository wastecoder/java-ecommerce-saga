package com.wastecoder.shopflow.payment.adapter.persistence.mapper;

import com.wastecoder.shopflow.payment.adapter.persistence.entity.PaymentEntity;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;
import com.wastecoder.shopflow.payment.testsupport.mother.PaymentMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEntityMapperTest {

	private final PaymentEntityMapper mapper = new PaymentEntityMapperImpl();

	@Test
	@DisplayName("Given null inputs, when mapping, then null is returned")
	void nullInputs_returnNull() {
		assertThat(mapper.toEntity(null)).isNull();
		assertThat(mapper.toDomain(null)).isNull();
	}

	@Test
	@DisplayName("Given an AUTHORIZED payment, when mapped to entity and back, then all fields round-trip")
	void roundTrip_authorized() {
		Payment domain = PaymentMother.anAuthorizedPayment();

		PaymentEntity entity = mapper.toEntity(domain);
		assertThat(entity.getId()).isEqualTo(domain.id());
		assertThat(entity.getOrderId()).isEqualTo(domain.orderId());
		assertThat(entity.getAmount()).isEqualByComparingTo(domain.amount());
		assertThat(entity.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
		assertThat(entity.getProviderRef()).isEqualTo(domain.providerRef());

		assertThat(mapper.toDomain(entity)).isEqualTo(domain);
	}

	@Test
	@DisplayName("Given a REFUNDED payment, when mapped to entity and back, then the status round-trips")
	void roundTrip_refunded() {
		Payment domain = PaymentMother.aRefundedPayment();

		assertThat(mapper.toDomain(mapper.toEntity(domain))).isEqualTo(domain);
	}
}
