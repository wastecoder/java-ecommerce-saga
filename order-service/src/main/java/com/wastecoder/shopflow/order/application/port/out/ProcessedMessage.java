package com.wastecoder.shopflow.order.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Records that an inbound message ({@code eventId}) was already handled by a given {@code consumer},
 * backing the idempotent-consumer dedup. Mirrors {@code ProcessedMessage(eventId, consumer, processedAt)}
 * from CHALLENGE §7. It is a messaging-infrastructure concept, not a business-domain model, so it lives by
 * the driven port rather than in {@code domain.model}.
 */
public record ProcessedMessage(UUID eventId, String consumer, Instant processedAt) {
}
