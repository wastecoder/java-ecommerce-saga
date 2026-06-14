package com.wastecoder.shopflow.inventory.adapter.messaging.out;

import com.wastecoder.shopflow.inventory.adapter.messaging.MessageType;
import com.wastecoder.shopflow.inventory.adapter.messaging.Topics;
import com.wastecoder.shopflow.inventory.adapter.messaging.payload.StockReplyPayload;
import com.wastecoder.shopflow.inventory.application.port.out.EventPublisher;
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
class InventoryEventPublisherImplTest {

	private static final UUID ORDER_ID = UUID.fromString("b1111111-1111-1111-1111-111111111111");

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private InventoryEventPublisherImpl publisher;

	@Test
	@DisplayName("Given an order, when stockReserved, then a StockReserved envelope is published to inventory.events keyed by the order id")
	void stockReserved_publishesReserved() {
		publisher.stockReserved(ORDER_ID);
		verifyPublished(MessageType.STOCK_RESERVED);
	}

	@Test
	@DisplayName("Given an order, when stockReservationFailed, then a StockReservationFailed envelope is published")
	void stockReservationFailed_publishesFailed() {
		publisher.stockReservationFailed(ORDER_ID);
		verifyPublished(MessageType.STOCK_RESERVATION_FAILED);
	}

	@Test
	@DisplayName("Given an order, when stockReleased, then a StockReleased envelope is published")
	void stockReleased_publishesReleased() {
		publisher.stockReleased(ORDER_ID);
		verifyPublished(MessageType.STOCK_RELEASED);
	}

	private void verifyPublished(String type) {
		ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher).publish(eq(Topics.INVENTORY_EVENTS), eq(ORDER_ID.toString()), eq(type),
				payload.capture());
		assertThat(payload.getValue()).isEqualTo(new StockReplyPayload(ORDER_ID));
	}
}
