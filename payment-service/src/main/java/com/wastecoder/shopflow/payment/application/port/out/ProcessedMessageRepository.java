package com.wastecoder.shopflow.payment.application.port.out;

import java.util.UUID;

/** Driven port for the {@code processed_messages} idempotency ledger. */
public interface ProcessedMessageRepository {

	boolean existsByEventIdAndConsumer(UUID eventId, String consumer);

	ProcessedMessage save(ProcessedMessage message);
}
