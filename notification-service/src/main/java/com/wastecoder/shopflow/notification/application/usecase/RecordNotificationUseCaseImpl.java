package com.wastecoder.shopflow.notification.application.usecase;

import com.wastecoder.shopflow.notification.application.port.in.RecordNotificationUseCase;
import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.application.viewmodel.NotificationCommand;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Records a notification for an order-lifecycle event and "sends" it through the mock channel (a log line).
 * The message is rendered from the {@link NotificationCommand}'s type, then the notification is persisted so
 * it can be inspected over REST.
 */
@Service
public class RecordNotificationUseCaseImpl implements RecordNotificationUseCase {

	private static final Logger log = LoggerFactory.getLogger(RecordNotificationUseCaseImpl.class);

	private final NotificationRepository repository;

	public RecordNotificationUseCaseImpl(NotificationRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void execute(NotificationCommand command) {
		String message = command.type().message(command.orderId(), command.totalAmount());
		Notification notification = repository.save(
				Notification.create(command.orderId(), command.customerId(), command.type(), message, Instant.now()));
		// Mock "send": the notification channel is a log line.
		log.info("Notification sent [{}] for order {}: {}", notification.type(), notification.orderId(), message);
	}
}
