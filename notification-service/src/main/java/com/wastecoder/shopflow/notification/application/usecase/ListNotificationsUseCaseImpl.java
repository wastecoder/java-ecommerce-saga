package com.wastecoder.shopflow.notification.application.usecase;

import com.wastecoder.shopflow.notification.application.port.in.ListNotificationsUseCase;
import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListNotificationsUseCaseImpl implements ListNotificationsUseCase {

	private final NotificationRepository repository;

	public ListNotificationsUseCaseImpl(NotificationRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> execute() {
		return repository.findAll();
	}
}
