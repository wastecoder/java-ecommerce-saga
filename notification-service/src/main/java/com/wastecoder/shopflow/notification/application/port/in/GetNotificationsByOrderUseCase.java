package com.wastecoder.shopflow.notification.application.port.in;

import com.wastecoder.shopflow.notification.domain.model.Notification;

import java.util.List;
import java.util.UUID;

/** Lists the notifications recorded for a given order, most recent first. */
public interface GetNotificationsByOrderUseCase {

	List<Notification> execute(UUID orderId);
}
