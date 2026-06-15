package com.wastecoder.shopflow.payment.application.usecase;

import com.wastecoder.shopflow.payment.application.port.in.AuthorizePaymentUseCase;
import com.wastecoder.shopflow.payment.application.port.out.PaymentDecision;
import com.wastecoder.shopflow.payment.application.port.out.PaymentEventPublisher;
import com.wastecoder.shopflow.payment.application.port.out.PaymentGateway;
import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.application.viewmodel.PaymentCommand;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Authorizes a payment for an order in response to a ProcessPayment command. The controllable PSP mock
 * (the {@link PaymentGateway}) decides approve/decline; the outcome is persisted as a {@link Payment} and
 * the matching reply (PaymentAuthorized / PaymentFailed) is published. The use case publishes its own reply,
 * mirroring the order saga coordinator, so the listener stays thin.
 *
 * <p>Partial idempotency via state guards (complementing the {@code eventId} dedup at the listener): a
 * redelivered command for an order that already has a payment republishes the matching reply without
 * charging again.
 */
@Service
public class AuthorizePaymentUseCaseImpl implements AuthorizePaymentUseCase {

	private static final Logger log = LoggerFactory.getLogger(AuthorizePaymentUseCaseImpl.class);

	private final PaymentRepository paymentRepository;
	private final PaymentGateway paymentGateway;
	private final PaymentEventPublisher eventPublisher;

	public AuthorizePaymentUseCaseImpl(PaymentRepository paymentRepository, PaymentGateway paymentGateway,
			PaymentEventPublisher eventPublisher) {
		this.paymentRepository = paymentRepository;
		this.paymentGateway = paymentGateway;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public void execute(PaymentCommand command) {
		UUID orderId = command.orderId();

		Optional<Payment> existing = paymentRepository.findByOrderId(orderId);
		if (existing.isPresent()) {
			republishExisting(orderId, existing.get());
			return;
		}

		PaymentDecision decision = paymentGateway.authorize(orderId, command.amount());
		if (decision.approved()) {
			paymentRepository.save(
					Payment.authorized(UUID.randomUUID(), orderId, command.amount(), decision.providerRef()));
			log.info("Payment authorized for order {} (ref {})", orderId, decision.providerRef());
			eventPublisher.paymentAuthorized(orderId);
		} else {
			paymentRepository.save(
					Payment.failed(UUID.randomUUID(), orderId, command.amount(), decision.providerRef()));
			log.info("Payment declined for order {} (ref {}, reason: {})", orderId, decision.providerRef(),
					decision.declineReason());
			eventPublisher.paymentFailed(orderId);
		}
	}

	private void republishExisting(UUID orderId, Payment payment) {
		switch (payment.status()) {
			case AUTHORIZED -> {
				log.info("ProcessPayment redelivered for already-authorized order {}; republishing PaymentAuthorized",
						orderId);
				eventPublisher.paymentAuthorized(orderId);
			}
			case FAILED -> {
				log.info("ProcessPayment redelivered for already-failed order {}; republishing PaymentFailed", orderId);
				eventPublisher.paymentFailed(orderId);
			}
			case REFUNDED -> log.info("ProcessPayment redelivered for already-refunded order {}; ignoring", orderId);
		}
	}
}
