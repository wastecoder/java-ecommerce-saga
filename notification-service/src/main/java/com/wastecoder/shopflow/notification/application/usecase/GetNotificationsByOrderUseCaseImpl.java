package com.wastecoder.shopflow.notification.application.usecase;

import com.wastecoder.shopflow.notification.application.port.in.GetNotificationsByOrderUseCase;
import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetNotificationsByOrderUseCaseImpl implements GetNotificationsByOrderUseCase {

	private final NotificationRepository repository;

	public GetNotificationsByOrderUseCaseImpl(NotificationRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> execute(UUID orderId) {
		return repository.findByOrderId(orderId);
	}
}
