package com.wastecoder.shopflow.notification.application.port.in;

import com.wastecoder.shopflow.notification.domain.model.Notification;

import java.util.List;

/** Lists every recorded notification, most recent first. */
public interface ListNotificationsUseCase {

	List<Notification> execute();
}
