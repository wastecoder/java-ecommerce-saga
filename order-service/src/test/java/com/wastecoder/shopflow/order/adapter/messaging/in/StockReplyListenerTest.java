package com.wastecoder.shopflow.order.adapter.messaging.in;

import com.wastecoder.shopflow.order.adapter.messaging.Consumers;
import com.wastecoder.shopflow.order.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.order.adapter.messaging.MessageType;
import com.wastecoder.shopflow.order.application.port.in.HandleStockReplyUseCase;
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
class StockReplyListenerTest {

	@Mock
	private HandleStockReplyUseCase useCase;

	@Mock
	private ProcessedMessageRepository processedMessages;

	private StockReplyListener listener;

	@BeforeEach
	void setUp() {
		// Real idempotency guard over a mocked ledger: with exists==false (default) it passes through to the
		// routing, so the routing assertions below still hold.
		listener = new StockReplyListener(useCase, new IdempotentMessageProcessor(processedMessages));
	}

	private static EventEnvelope envelope(String type) {
		return new EventEnvelope(UUID.randomUUID(), type, OrderMother.ORDER_ID.toString(), Instant.now(), null);
	}

	@Test
	@DisplayName("Given a StockReserved event, when consumed, then onStockReserved is invoked with the order id")
	void routesStockReserved() {
		listener.onMessage(envelope(MessageType.STOCK_RESERVED));
		verify(useCase).onStockReserved(OrderMother.ORDER_ID);
	}

	@Test
	@DisplayName("Given a StockReservationFailed event, when consumed, then onStockReservationFailed is invoked")
	void routesStockReservationFailed() {
		listener.onMessage(envelope(MessageType.STOCK_RESERVATION_FAILED));
		verify(useCase).onStockReservationFailed(OrderMother.ORDER_ID);
	}

	@Test
	@DisplayName("Given a StockReleased event, when consumed, then onStockReleased is invoked")
	void routesStockReleased() {
		listener.onMessage(envelope(MessageType.STOCK_RELEASED));
		verify(useCase).onStockReleased(OrderMother.ORDER_ID);
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
		when(processedMessages.existsByEventIdAndConsumer(any(), eq(Consumers.STOCK_REPLY))).thenReturn(true);

		listener.onMessage(envelope(MessageType.STOCK_RESERVED));

		verifyNoInteractions(useCase);
	}
}
