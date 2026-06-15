package com.wastecoder.shopflow.payment.adapter.messaging.out;

import com.wastecoder.shopflow.payment.adapter.messaging.MessageType;
import com.wastecoder.shopflow.payment.adapter.messaging.Topics;
import com.wastecoder.shopflow.payment.adapter.messaging.payload.PaymentReplyPayload;
import com.wastecoder.shopflow.payment.application.port.out.EventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherImplTest {

	private static final UUID ORDER_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private PaymentEventPublisherImpl publisher;

	@Test
	@DisplayName("Given an order, when paymentAuthorized, then a PaymentAuthorized envelope is published to payment.events keyed by the order id")
	void paymentAuthorized_publishesAuthorized() {
		publisher.paymentAuthorized(ORDER_ID);
		verifyPublished(MessageType.PAYMENT_AUTHORIZED);
	}

	@Test
	@DisplayName("Given an order, when paymentFailed, then a PaymentFailed envelope is published")
	void paymentFailed_publishesFailed() {
		publisher.paymentFailed(ORDER_ID);
		verifyPublished(MessageType.PAYMENT_FAILED);
	}

	@Test
	@DisplayName("Given an order, when paymentRefunded, then a PaymentRefunded envelope is published")
	void paymentRefunded_publishesRefunded() {
		publisher.paymentRefunded(ORDER_ID);
		verifyPublished(MessageType.PAYMENT_REFUNDED);
	}

	private void verifyPublished(String type) {
		ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher).publish(eq(Topics.PAYMENT_EVENTS), eq(ORDER_ID.toString()), eq(type),
				payload.capture());
		assertThat(payload.getValue()).isEqualTo(new PaymentReplyPayload(ORDER_ID));
	}
}
