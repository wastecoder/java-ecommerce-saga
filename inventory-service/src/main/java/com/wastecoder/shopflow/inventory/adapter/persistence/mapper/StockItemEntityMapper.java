package com.wastecoder.shopflow.inventory.adapter.persistence.mapper;

import com.wastecoder.shopflow.inventory.adapter.persistence.entity.StockItemEntity;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StockItemEntityMapper {

	StockItemEntity toEntity(StockItem stockItem);

	// reserve/release are immutable "wither" behaviour methods, not properties; MapStruct reads their
	// single-arg self-returning signature as fluent setters, so they are explicitly ignored.
	@Mapping(target = "reserve", ignore = true)
	@Mapping(target = "release", ignore = true)
	StockItem toDomain(StockItemEntity entity);
}
