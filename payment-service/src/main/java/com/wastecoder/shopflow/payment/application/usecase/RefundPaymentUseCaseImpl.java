package com.wastecoder.shopflow.payment.application.usecase;

import com.wastecoder.shopflow.payment.application.port.in.RefundPaymentUseCase;
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
 * Refunds a previously authorized payment in response to a RefundPayment compensation command. The persisted
 * payment is the source of truth: an AUTHORIZED payment is refunded at the PSP, marked REFUNDED and a
 * PaymentRefunded reply is published. A redelivered command for an already-REFUNDED order republishes the
 * acknowledgement (idempotent); an unknown or already-FAILED payment is a no-op (there is nothing to refund).
 *
 * <p>Note: the order-service does not emit RefundPayment in the current saga, so this flow is exercised by
 * payment-service integration tests rather than end-to-end (see PROGRESS.md, Fase 3).
 */
@Service
public class RefundPaymentUseCaseImpl implements RefundPaymentUseCase {

	private static final Logger log = LoggerFactory.getLogger(RefundPaymentUseCaseImpl.class);

	private final PaymentRepository paymentRepository;
	private final PaymentGateway paymentGateway;
	private final PaymentEventPublisher eventPublisher;

	public RefundPaymentUseCaseImpl(PaymentRepository paymentRepository, PaymentGateway paymentGateway,
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
		if (existing.isEmpty()) {
			log.warn("RefundPayment for unknown payment of order {}; nothing to refund", orderId);
			return;
		}

		Payment payment = existing.get();
		switch (payment.status()) {
			case REFUNDED -> {
				log.info("RefundPayment redelivered for already-refunded order {}; republishing PaymentRefunded",
						orderId);
				eventPublisher.paymentRefunded(orderId);
			}
			case AUTHORIZED -> {
				PaymentDecision decision = paymentGateway.refund(orderId, payment.providerRef(), payment.amount());
				paymentRepository.save(payment.refund());
				log.info("Payment refunded for order {} (ref {})", orderId, decision.providerRef());
				eventPublisher.paymentRefunded(orderId);
			}
			case FAILED -> log.warn("RefundPayment for a failed payment of order {}; nothing to refund", orderId);
		}
	}
}
