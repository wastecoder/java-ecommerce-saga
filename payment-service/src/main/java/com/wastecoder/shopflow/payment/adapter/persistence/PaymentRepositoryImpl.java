package com.wastecoder.shopflow.payment.adapter.persistence;

import com.wastecoder.shopflow.payment.adapter.persistence.database.PaymentJpaDatabase;
import com.wastecoder.shopflow.payment.adapter.persistence.mapper.PaymentEntityMapper;
import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

	private final PaymentJpaDatabase database;
	private final PaymentEntityMapper mapper;

	public PaymentRepositoryImpl(PaymentJpaDatabase database, PaymentEntityMapper mapper) {
		this.database = database;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public Payment save(Payment payment) {
		return mapper.toDomain(database.save(mapper.toEntity(payment)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Payment> findByOrderId(UUID orderId) {
		return database.findByOrderId(orderId).map(mapper::toDomain);
	}
}
