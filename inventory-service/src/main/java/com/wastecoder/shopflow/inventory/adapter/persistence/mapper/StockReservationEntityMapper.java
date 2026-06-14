package com.wastecoder.shopflow.inventory.adapter.persistence.mapper;

import com.wastecoder.shopflow.inventory.adapter.persistence.entity.StockReservationEntity;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StockReservationEntityMapper {

	StockReservationEntity toEntity(StockReservation reservation);

	StockReservation toDomain(StockReservationEntity entity);
}
