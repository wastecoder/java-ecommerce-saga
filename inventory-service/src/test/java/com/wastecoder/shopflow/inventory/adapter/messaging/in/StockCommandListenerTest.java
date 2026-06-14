package com.wastecoder.shopflow.inventory.adapter.messaging.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.shopflow.inventory.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.inventory.adapter.messaging.MessageType;
import com.wastecoder.shopflow.inventory.application.port.in.ReleaseStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.in.ReserveStockUseCase;
import com.wastecoder.shopflow.inventory.application.viewmodel.StockCommand;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockCommandMother;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockCommandListenerTest {

	@Mock
	private ReserveStockUseCase reserveStockUseCase;

	@Mock
	private ReleaseStockUseCase releaseStockUseCase;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private StockCommandListener listener() {
		return new StockCommandListener(reserveStockUseCase, releaseStockUseCase, objectMapper);
	}

	private static EventEnvelope envelope(String type, Object payload) {
		return new EventEnvelope(UUID.randomUUID(), type, StockCommandMother.ORDER_ID.toString(), Instant.now(),
				payload);
	}

	@Test
	@DisplayName("Given a ReserveStock command, when consumed, then the Map payload is converted to a StockCommand and ReserveStock is invoked")
	void routesAndConvertsReserve() {
		Map<String, Object> payload = StockCommandMother.asPayloadMap(StockCommandMother.aValidCommand());

		listener().onMessage(envelope(MessageType.RESERVE_STOCK, payload));

		ArgumentCaptor<StockCommand> captor = ArgumentCaptor.forClass(StockCommand.class);
		verify(reserveStockUseCase).execute(captor.capture());
		assertThat(captor.getValue().orderId()).isEqualTo(StockCommandMother.ORDER_ID);
		assertThat(captor.getValue().items()).hasSize(1);
		assertThat(captor.getValue().items().get(0).productId()).isEqualTo(StockItemMother.PRODUCT_ID);
		assertThat(captor.getValue().items().get(0).quantity()).isEqualTo(2);
		verifyNoInteractions(releaseStockUseCase);
	}

	@Test
	@DisplayName("Given a ReleaseStock command, when consumed, then the converted StockCommand is passed to ReleaseStock")
	void routesAndConvertsRelease() {
		Map<String, Object> payload = StockCommandMother.asPayloadMap(StockCommandMother.aValidCommand());

		listener().onMessage(envelope(MessageType.RELEASE_STOCK, payload));

		ArgumentCaptor<StockCommand> captor = ArgumentCaptor.forClass(StockCommand.class);
		verify(releaseStockUseCase).execute(captor.capture());
		assertThat(captor.getValue().orderId()).isEqualTo(StockCommandMother.ORDER_ID);
		verifyNoInteractions(reserveStockUseCase);
	}

	@Test
	@DisplayName("Given an unhandled command type, when consumed, then neither use case is invoked")
	void ignoresUnknownType() {
		listener().onMessage(envelope("SomethingElse", Map.of()));

		verifyNoInteractions(reserveStockUseCase, releaseStockUseCase);
	}
}
