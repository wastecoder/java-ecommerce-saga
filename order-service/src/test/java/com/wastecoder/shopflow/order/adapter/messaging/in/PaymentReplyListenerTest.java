package com.wastecoder.shopflow.order.adapter.messaging.in;

import com.wastecoder.shopflow.order.adapter.messaging.Consumers;
import com.wastecoder.shopflow.order.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.order.adapter.messaging.MessageType;
import com.wastecoder.shopflow.order.application.port.in.HandlePaymentReplyUseCase;
import com.wastecoder.shopflow.order.application.port.out.ProcessedMessageRepository;
import com.wastecoder.shopflow.order.testsupport.mother.OrderMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReplyListenerTest {

	@Mock
	private HandlePaymentReplyUseCase useCase;

	@Mock
	private ProcessedMessageRepository processedMessages;

	private PaymentReplyListener listener;

	@BeforeEach
	void setUp() {
		listener = new PaymentReplyListener(useCase, new IdempotentMessageProcessor(processedMessages));
	}

	private static EventEnvelope envelope(String type) {
		return new EventEnvelope(UUID.randomUUID(), type, OrderMother.ORDER_ID.toString(), Instant.now(), null);
	}

	@Test
	@DisplayName("Given a PaymentAuthorized event, when consumed, then onPaymentAuthorized is invoked with the order id")
	void routesPaymentAuthorized() {
		listener.onMessage(envelope(MessageType.PAYMENT_AUTHORIZED));
		verify(useCase).onPaymentAuthorized(OrderMother.ORDER_ID);
	}

	@Test
	@DisplayName("Given a PaymentFailed event, when consumed, then onPaymentFailed is invoked")
	void routesPaymentFailed() {
		listener.onMessage(envelope(MessageType.PAYMENT_FAILED));
		verify(useCase).onPaymentFailed(OrderMother.ORDER_ID);
	}

	@Test
	@DisplayName("Given an unhandled event type, when consumed, then the use case is not invoked")
	void ignoresUnknownType() {
		listener.onMessage(envelope("SomethingElse"));
		verifyNoInteractions(useCase);
	}

	@Test
	@DisplayName("Given an event already processed by this consumer, when consumed again, then it is skipped and the use case is not invoked")
	void duplicateEvent_isSkipped() {
		when(processedMessages.existsByEventIdAndConsumer(any(), eq(Consumers.PAYMENT_REPLY))).thenReturn(true);

		listener.onMessage(envelope(MessageType.PAYMENT_AUTHORIZED));

		verifyNoInteractions(useCase);
	}
}
