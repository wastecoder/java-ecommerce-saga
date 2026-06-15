package com.wastecoder.shopflow.notification.adapter.persistence;

import com.wastecoder.shopflow.notification.adapter.persistence.database.NotificationJpaDatabase;
import com.wastecoder.shopflow.notification.adapter.persistence.mapper.NotificationEntityMapper;
import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

	private final NotificationJpaDatabase database;
	private final NotificationEntityMapper mapper;

	public NotificationRepositoryImpl(NotificationJpaDatabase database, NotificationEntityMapper mapper) {
		this.database = database;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public Notification save(Notification notification) {
		return mapper.toDomain(database.save(mapper.toEntity(notification)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> findAll() {
		return database.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> findByOrderId(UUID orderId) {
		return database.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(mapper::toDomain).toList();
	}
}
