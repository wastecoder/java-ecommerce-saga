package com.wastecoder.shopflow.notification.application.port.in;

import com.wastecoder.shopflow.notification.application.viewmodel.NotificationCommand;

/** Records (and mock-sends) a notification for an order-lifecycle event. */
public interface RecordNotificationUseCase {

	void execute(NotificationCommand command);
}
