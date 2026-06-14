package com.wastecoder.shopflow.inventory.application.usecase;

import com.wastecoder.shopflow.inventory.application.port.in.ReleaseStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.out.InventoryEventPublisher;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.application.port.out.StockReservationRepository;
import com.wastecoder.shopflow.inventory.application.viewmodel.StockCommand;
import com.wastecoder.shopflow.inventory.domain.exception.StockItemNotFoundException;
import com.wastecoder.shopflow.inventory.domain.model.ReservationStatus;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.inventory.domain.model.StockReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Releases the stock previously reserved for an order in response to a ReleaseStock compensation command.
 * The persisted RESERVED reservations are the source of truth: each is reversed (stock returned, the
 * reservation marked RELEASED). When no RESERVED reservations exist (already released, or unknown order)
 * the StockReleased acknowledgement is still published, which makes the compensation idempotent.
 */
@Service
public class ReleaseStockUseCaseImpl implements ReleaseStockUseCase {

	private static final Logger log = LoggerFactory.getLogger(ReleaseStockUseCaseImpl.class);

	private final StockRepository stockRepository;
	private final StockReservationRepository reservationRepository;
	private final InventoryEventPublisher eventPublisher;

	public ReleaseStockUseCaseImpl(StockRepository stockRepository,
			StockReservationRepository reservationRepository, InventoryEventPublisher eventPublisher) {
		this.stockRepository = stockRepository;
		this.reservationRepository = reservationRepository;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public void execute(StockCommand command) {
		UUID orderId = command.orderId();

		List<StockReservation> reserved =
				reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
		for (StockReservation reservation : reserved) {
			StockItem stock = stockRepository.findByProductId(reservation.productId())
					.orElseThrow(() -> new StockItemNotFoundException(reservation.productId()));
			stockRepository.save(stock.release(reservation.quantity()));
			reservationRepository.save(reservation.release());
		}
		log.info("Released {} reservation(s) for order {}", reserved.size(), orderId);
		eventPublisher.stockReleased(orderId);
	}
}
