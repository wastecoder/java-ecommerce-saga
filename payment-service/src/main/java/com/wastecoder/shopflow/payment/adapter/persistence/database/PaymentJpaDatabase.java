package com.wastecoder.shopflow.payment.adapter.persistence.database;

import com.wastecoder.shopflow.payment.adapter.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaDatabase extends JpaRepository<PaymentEntity, UUID> {

	Optional<PaymentEntity> findByOrderId(UUID orderId);
}
