package com.wastecoder.shopflow.notification.adapter.persistence.mapper;

import com.wastecoder.shopflow.notification.adapter.persistence.entity.NotificationEntity;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationEntityMapper {

	NotificationEntity toEntity(Notification notification);

	Notification toDomain(NotificationEntity entity);
}
