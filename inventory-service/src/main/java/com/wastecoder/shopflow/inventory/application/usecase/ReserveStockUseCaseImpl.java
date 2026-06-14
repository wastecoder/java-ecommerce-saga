package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.in.ReserveStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.out.InventoryEventPublisher;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.application.port.out.StockReservationRepository;
import com.wastecoder.shopflow.inventory.application.viewmodel.StockCommand;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reserves stock for an order in response to a ReserveStock command. The reservation is all-or-nothing:
 * every line must have enough available stock, otherwise nothing is reserved and a FAILED reservation is
 * recorded per line (audit trail). On success the stock is decremented and a RESERVED reservation is
 * recorded per line. The use case publishes its own reply (StockReserved / StockReservationFailed),
 * mirroring the order saga coordinator, so the listener stays thin.
 *
 * <p>Partial idempotency via state guards (full {@code eventId} dedup is Fase 2, item 4): a redelivered
 * command for an order that already has RESERVED (or FAILED) reservations republishes the matching reply
 * without touching stock again.
 */
@Service
public class ReserveStockUseCaseImpl implements ReserveStockUseCase {

	private static final Logger log = LoggerFactory.getLogger(ReserveStockUseCaseImpl.class);

	private final StockRepository stockRepository;
	private final StockReservationRepository reservationRepository;
	private final InventoryEventPublisher eventPublisher;

	public ReserveStockUseCaseImpl(StockRepository stockRepository,
			StockReservationRepository reservationRepository, InventoryEventPublisher eventPublisher) {
		this.stockRepository = stockRepository;
		this.reservationRepository = reservationRepository;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public void execute(StockCommand command) {
		UUID orderId = command.orderId();

		List<StockReservation> existing = reservationRepository.findByOrderId(orderId);
		if (existing.stream().anyMatch(r -> r.status() == ReservationStatus.RESERVED)) {
			log.info("ReserveStock redelivered for already-reserved order {}; republishing StockReserved", orderId);
			eventPublisher.stockReserved(orderId);
			return;
		}
		if (existing.stream().anyMatch(r -> r.status() == ReservationStatus.FAILED)) {
			log.info("ReserveStock redelivered for already-failed order {}; republishing StockReservationFailed",
					orderId);
			eventPublisher.stockReservationFailed(orderId);
			return;
		}

		// Validate every line before applying any change (all-or-nothing).
		List<StockItem> toReserve = new ArrayList<>();
		for (StockCommand.Item item : command.items()) {
			Optional<StockItem> stock = stockRepository.findByProductId(item.productId());
			if (stock.isEmpty() || !stock.get().canReserve(item.quantity())) {
				recordFailure(command);
				log.info("ReserveStock failed for order {} (insufficient or unknown product {})", orderId,
						item.productId());
				eventPublisher.stockReservationFailed(orderId);
				return;
			}
			toReserve.add(stock.get());
		}

		for (int i = 0; i < command.items().size(); i++) {
			StockCommand.Item item = command.items().get(i);
			stockRepository.save(toReserve.get(i).reserve(item.quantity()));
			reservationRepository.save(
					StockReservation.reserve(UUID.randomUUID(), orderId, item.productId(), item.quantity()));
		}
		log.info("Stock reserved for order {} ({} item(s))", orderId, command.items().size());
		eventPublisher.stockReserved(orderId);
	}

	private void recordFailure(StockCommand command) {
		for (StockCommand.Item item : command.items()) {
			reservationRepository.save(
					StockReservation.failed(UUID.randomUUID(), command.orderId(), item.productId(), item.quantity()));
		}
	}
}
