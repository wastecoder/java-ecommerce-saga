package com.wastecoder.shopflow.inventory.adapter.messaging.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.shopflow.inventory.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.inventory.adapter.messaging.MessageType;
import com.wastecoder.shopflow.inventory.adapter.messaging.Topics;
import com.wastecoder.shopflow.inventory.application.port.in.ReleaseStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.in.ReserveStockUseCase;
import com.wastecoder.shopflow.inventory.application.viewmodel.StockCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the saga commands from {@code inventory.commands} and routes them by event type. Thin
 * adapter: it converts the type-agnostic envelope payload (a {@code Map}, since type headers are off)
 * into a {@link StockCommand} and delegates to the use cases, which own the business logic and the reply.
 */
@Component
public class StockCommandListener {

	private static final Logger log = LoggerFactory.getLogger(StockCommandListener.class);

	private final ReserveStockUseCase reserveStockUseCase;
	private final ReleaseStockUseCase releaseStockUseCase;
	private final ObjectMapper objectMapper;

	public StockCommandListener(ReserveStockUseCase reserveStockUseCase, ReleaseStockUseCase releaseStockUseCase,
			ObjectMapper objectMapper) {
		this.reserveStockUseCase = reserveStockUseCase;
		this.releaseStockUseCase = releaseStockUseCase;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = Topics.INVENTORY_COMMANDS)
	public void onMessage(EventEnvelope envelope) {
		switch (envelope.type()) {
			case MessageType.RESERVE_STOCK -> reserveStockUseCase.execute(toCommand(envelope));
			case MessageType.RELEASE_STOCK -> releaseStockUseCase.execute(toCommand(envelope));
			default -> log.warn("Ignoring unhandled inventory command type '{}' for order {}", envelope.type(),
					envelope.orderId());
		}
	}

	private StockCommand toCommand(EventEnvelope envelope) {
		return objectMapper.convertValue(envelope.payload(), StockCommand.class);
	}
}
