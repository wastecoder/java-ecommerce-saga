package com.wastecoder.shopflow.inventory.adapter.messaging.in;

import com.wastecoder.shopflow.inventory.application.port.out.ProcessedMessage;
import com.wastecoder.shopflow.inventory.application.port.out.ProcessedMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotent-consumer guard (inbox pattern). Runs {@code handler} at most once per
 * {@code (eventId, consumer)}: the dedup marker and the handler's business writes commit in the SAME
 * transaction, so a handler failure rolls the marker back and the message stays eligible for retry/DLT. The
 * composite primary key {@code (event_id, consumer)} is the backstop against a concurrent duplicate (rare,
 * since {@code key=orderId} serializes a partition).
 *
 * <p>It lives in the messaging adapter, not the application layer, on purpose: deduplicating at-least-once
 * deliveries is a delivery concern, not business logic.
 */
@Component
public class IdempotentMessageProcessor {

	private static final Logger log = LoggerFactory.getLogger(IdempotentMessageProcessor.class);

	private final ProcessedMessageRepository repository;

	public IdempotentMessageProcessor(ProcessedMessageRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void processOnce(UUID eventId, String consumer, Runnable handler) {
		if (repository.existsByEventIdAndConsumer(eventId, consumer)) {
			log.debug("Skipping duplicate event {} already processed by {}", eventId, consumer);
			return;
		}
		handler.run();
		repository.save(new ProcessedMessage(eventId, consumer, Instant.now()));
	}
}
