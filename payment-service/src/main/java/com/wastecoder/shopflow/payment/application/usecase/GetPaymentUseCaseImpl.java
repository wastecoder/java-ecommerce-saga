package com.wastecoder.shopflow.payment.application.usecase;

import com.wastecoder.shopflow.payment.application.port.in.GetPaymentUseCase;
import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.domain.exception.PaymentNotFoundException;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetPaymentUseCaseImpl implements GetPaymentUseCase {

	private final PaymentRepository repository;

	public GetPaymentUseCaseImpl(PaymentRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Payment execute(UUID orderId) {
		return repository.findByOrderId(orderId)
				.orElseThrow(() -> new PaymentNotFoundException(orderId));
	}
}
