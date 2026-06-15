package com.wastecoder.shopflow.payment.adapter.persistence.mapper;

import com.wastecoder.shopflow.payment.adapter.persistence.entity.PaymentEntity;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentEntityMapper {

	PaymentEntity toEntity(Payment payment);

	Payment toDomain(PaymentEntity entity);
}
