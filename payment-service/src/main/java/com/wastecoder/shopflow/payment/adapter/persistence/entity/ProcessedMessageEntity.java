package com.wastecoder.shopflow.payment.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_messages")
@IdClass(ProcessedMessageId.class)
@Getter
@Setter
@NoArgsConstructor
public class ProcessedMessageEntity {

	@Id
	private UUID eventId;

	@Id
	@Column(length = 64)
	private String consumer;

	@Column(nullable = false)
	private Instant processedAt;
}
