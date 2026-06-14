package com.wastecoder.shopflow.order.adapter.persistence;

import com.wastecoder.shopflow.order.adapter.persistence.database.ProcessedMessageJpaDatabase;
import com.wastecoder.shopflow.order.adapter.persistence.entity.ProcessedMessageEntity;
import com.wastecoder.shopflow.order.application.port.out.ProcessedMessage;
import com.wastecoder.shopflow.order.application.port.out.ProcessedMessageRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public class ProcessedMessageRepositoryImpl implements ProcessedMessageRepository {

	private final ProcessedMessageJpaDatabase database;

	public ProcessedMessageRepositoryImpl(ProcessedMessageJpaDatabase database) {
		this.database = database;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEventIdAndConsumer(UUID eventId, String consumer) {
		return database.existsByEventIdAndConsumer(eventId, consumer);
	}

	@Override
	@Transactional
	public ProcessedMessage save(ProcessedMessage message) {
		ProcessedMessageEntity entity = new ProcessedMessageEntity();
		entity.setEventId(message.eventId());
		entity.setConsumer(message.consumer());
		entity.setProcessedAt(message.processedAt());
		ProcessedMessageEntity saved = database.save(entity);
		return new ProcessedMessage(saved.getEventId(), saved.getConsumer(), saved.getProcessedAt());
	}
}
