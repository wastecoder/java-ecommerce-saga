package com.wastecoder.shopflow.payment.adapter.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers the composite-key value semantics Hibernate relies on for {@code @IdClass}. */
class ProcessedMessageIdTest {

	private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER_EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Test
	@DisplayName("Given two ids with the same event id and consumer, when compared, then they are equal with the same hash code")
	void equalWhenSameFields() {
		ProcessedMessageId one = new ProcessedMessageId(EVENT_ID, "payment.payment-command");
		ProcessedMessageId two = new ProcessedMessageId(EVENT_ID, "payment.payment-command");

		assertThat(one).isEqualTo(two);
		assertThat(one).hasSameHashCodeAs(two);
		assertThat(one.getEventId()).isEqualTo(EVENT_ID);
		assertThat(one.getConsumer()).isEqualTo("payment.payment-command");
	}

	@Test
	@DisplayName("Given ids differing by event id or by consumer, when compared, then they are not equal")
	void notEqualWhenAnyFieldDiffers() {
		ProcessedMessageId base = new ProcessedMessageId(EVENT_ID, "payment.payment-command");

		assertThat(base).isNotEqualTo(new ProcessedMessageId(OTHER_EVENT_ID, "payment.payment-command"));
		assertThat(base).isNotEqualTo(new ProcessedMessageId(EVENT_ID, "payment.other-consumer"));
		assertThat(base).isNotEqualTo(null);
		assertThat(base).isNotEqualTo("not-an-id");
		assertThat(base).isEqualTo(base);
	}
}
