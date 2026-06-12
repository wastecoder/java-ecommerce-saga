package com.wastecoder.shopflow.inventory.adapter.persistence.mapper;

import com.wastecoder.shopflow.inventory.adapter.persistence.entity.StockItemEntity;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StockItemEntityMapper {

	StockItemEntity toEntity(StockItem stockItem);

	StockItem toDomain(StockItemEntity entity);
}
