package com.wastecoder.shopflow.notification.application.port.out;

import com.wastecoder.shopflow.notification.domain.model.Notification;

import java.util.List;
import java.util.UUID;

/** Driven port for persisting and querying recorded notifications. */
public interface NotificationRepository {

	Notification save(Notification notification);

	List<Notification> findAll();

	List<Notification> findByOrderId(UUID orderId);
}
