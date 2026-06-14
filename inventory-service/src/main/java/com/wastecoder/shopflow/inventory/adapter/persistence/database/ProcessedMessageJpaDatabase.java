package com.wastecoder.shopflow.inventory.adapter.persistence.database;

import com.wastecoder.shopflow.inventory.adapter.persistence.entity.ProcessedMessageEntity;
import com.wastecoder.shopflow.inventory.adapter.persistence.entity.ProcessedMessageId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedMessageJpaDatabase extends JpaRepository<ProcessedMessageEntity, ProcessedMessageId> {

	boolean existsByEventIdAndConsumer(UUID eventId, String consumer);
}
