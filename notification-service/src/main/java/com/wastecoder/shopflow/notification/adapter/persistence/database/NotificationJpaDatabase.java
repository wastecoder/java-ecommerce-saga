package com.wastecoder.shopflow.notification.adapter.persistence.database;

import com.wastecoder.shopflow.notification.adapter.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationJpaDatabase extends JpaRepository<NotificationEntity, UUID> {

	List<NotificationEntity> findAllByOrderByCreatedAtDesc();

	List<NotificationEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
