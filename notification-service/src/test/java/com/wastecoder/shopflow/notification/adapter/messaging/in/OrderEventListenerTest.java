package com.wastecoder.shopflow.notification.adapter.messaging.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.shopflow.notification.adapter.messaging.Consumers;
import com.wastecoder.shopflow.notification.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.notification.adapter.messaging.MessageType;
import com.wastecoder.shopflow.notification.application.port.in.RecordNotificationUseCase;
import com.wastecoder.shopflow.notification.application.port.out.ProcessedMessageRepository;
import com.wastecoder.shopflow.notification.application.viewmodel.NotificationCommand;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import com.wastecoder.shopflow.notification.testsupport.mother.OrderEventMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

	@Mock
	private RecordNotificationUseCase recordNotificationUseCase;

	@Mock
	private ProcessedMessageRepository processedMessages;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private OrderEventListener listener() {
		// Real idempotency guard over a mocked ledger: exists==false (default) passes through to routing.
		return new OrderEventListener(recordNotificationUseCase, objectMapper,
				new IdempotentMessageProcessor(processedMessages));
	}

	private static EventEnvelope envelope(String type, Object payload) {
		return new EventEnvelope(UUID.randomUUID(), type, OrderEventMother.ORDER_ID.toString(), Instant.now(), payload);
	}

	@Test
	@DisplayName("Given an OrderConfirmed event, when consumed, then a confirmed notification command is recorded")
	void routesConfirmed() {
		listener().onMessage(envelope(MessageType.ORDER_CONFIRMED, OrderEventMother.aPayloadMap("CONFIRMED")));

		ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
		verify(recordNotificationUseCase).execute(captor.capture());
		NotificationCommand command = captor.getValue();
		assertThat(command.type()).isEqualTo(NotificationType.ORDER_CONFIRMED);
		assertThat(command.orderId()).isEqualTo(OrderEventMother.ORDER_ID);
		assertThat(command.customerId()).isEqualTo(OrderEventMother.CUSTOMER_ID);
		assertThat(command.totalAmount()).isEqualByComparingTo(OrderEventMother.TOTAL_AMOUNT);
	}

	@Test
	@DisplayName("Given an OrderCancelled event, when consumed, then a cancelled notification command is recorded")
	void routesCancelled() {
		listener().onMessage(envelope(MessageType.ORDER_CANCELLED, OrderEventMother.aPayloadMap("CANCELLED")));

		ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
		verify(recordNotificationUseCase).execute(captor.capture());
		assertThat(captor.getValue().type()).isEqualTo(NotificationType.ORDER_CANCELLED);
	}

	@Test
	@DisplayName("Given an OrderCreated event, when consumed, then a created notification command is recorded")
	void routesCreated() {
		listener().onMessage(envelope(MessageType.ORDER_CREATED, OrderEventMother.aPayloadMap("PENDING")));

		ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
		verify(recordNotificationUseCase).execute(captor.capture());
		assertThat(captor.getValue().type()).isEqualTo(NotificationType.ORDER_CREATED);
	}

	@Test
	@DisplayName("Given an OrderRejected event, when consumed, then a rejected notification command is recorded")
	void routesRejected() {
		listener().onMessage(envelope(MessageType.ORDER_REJECTED, OrderEventMother.aPayloadMap("REJECTED")));

		ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
		verify(recordNotificationUseCase).execute(captor.capture());
		assertThat(captor.getValue().type()).isEqualTo(NotificationType.ORDER_REJECTED);
	}

	@Test
	@DisplayName("Given an unhandled event type, when consumed, then no notification is recorded")
	void ignoresUnknownType() {
		listener().onMessage(envelope("SomethingElse", Map.of()));

		verifyNoInteractions(recordNotificationUseCase);
	}

	@Test
	@DisplayName("Given an event already processed by this consumer, when consumed again, then it is skipped")
	void duplicateEvent_isSkipped() {
		when(processedMessages.existsByEventIdAndConsumer(any(), eq(Consumers.ORDER_EVENT))).thenReturn(true);

		listener().onMessage(envelope(MessageType.ORDER_CONFIRMED, OrderEventMother.aPayloadMap("CONFIRMED")));

		verifyNoInteractions(recordNotificationUseCase);
	}
}
