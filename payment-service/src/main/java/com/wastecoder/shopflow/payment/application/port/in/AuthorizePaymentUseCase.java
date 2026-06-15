package com.wastecoder.shopflow.payment.application.port.in;

import com.wastecoder.shopflow.payment.application.viewmodel.PaymentCommand;

public interface AuthorizePaymentUseCase {

	void execute(PaymentCommand command);
}
