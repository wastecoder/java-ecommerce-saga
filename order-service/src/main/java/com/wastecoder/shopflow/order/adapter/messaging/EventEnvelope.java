package com.wastecoder.shopflow.order.adapter.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire format for every message exchanged over Kafka (commands and events).
 *
 * <p>Serialized as a plain JSON object {@code { eventId, type, orderId, occurredAt, payload }} with
 * no Spring type-info headers, so producers and consumers stay decoupled from each other's Java
 * packages. {@code payload} is intentionally {@code Object}: the envelope is type-agnostic here, and
 * consumers convert it to a concrete DTO based on {@code type}.
 */
public record EventEnvelope(

		UUID eventId,

		String type,

		String orderId,

		Instant occurredAt,

		Object payload
) { }
