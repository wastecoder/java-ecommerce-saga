package com.wastecoder.shopflow.notification.adapter.persistence.entity;

import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class NotificationEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID orderId;

	@Column(nullable = false)
	private UUID customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private NotificationType type;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(nullable = false)
	private Instant createdAt;
}
