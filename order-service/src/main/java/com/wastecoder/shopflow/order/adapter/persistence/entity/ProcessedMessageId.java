package com.wastecoder.shopflow.order.adapter.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/** Composite primary key {@code (eventId, consumer)} for {@link ProcessedMessageEntity} ({@code @IdClass}). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProcessedMessageId implements Serializable {

	private UUID eventId;

	private String consumer;
}
