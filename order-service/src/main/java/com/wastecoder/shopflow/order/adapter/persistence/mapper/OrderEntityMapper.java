package com.wastecoder.shopflow.order.adapter.persistence.mapper;

import com.wastecoder.shopflow.order.adapter.persistence.entity.OrderEntity;
import com.wastecoder.shopflow.order.adapter.persistence.entity.OrderItemEntity;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderEntityMapper {

	OrderEntity toEntity(Order order);

	Order toDomain(OrderEntity entity);

	@Mapping(target = "order", ignore = true) // back-reference wired in linkItems
	OrderItemEntity toItemEntity(OrderItem item);

	OrderItem toItemDomain(OrderItemEntity entity);

	@AfterMapping
	default void linkItems(@MappingTarget OrderEntity entity) {
		if (entity.getItems() != null) {
			entity.getItems().forEach(item -> item.setOrder(entity));
		}
	}
}
