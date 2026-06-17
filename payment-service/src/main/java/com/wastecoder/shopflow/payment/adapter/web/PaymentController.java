package com.wastecoder.shopflow.payment.adapter.web;

import com.wastecoder.shopflow.payment.adapter.web.dto.response.PaymentResponse;
import com.wastecoder.shopflow.payment.application.port.in.GetPaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payment Endpoints", description = "Operations to query payments by order.")
public class PaymentController {

	private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

	private final GetPaymentUseCase getPaymentUseCase;

	public PaymentController(GetPaymentUseCase getPaymentUseCase) {
		this.getPaymentUseCase = getPaymentUseCase;
	}

	@GetMapping("/{orderId}")
	@Operation(
			summary = "Retrieve the payment of an order",
			description = "Returns the payment recorded for the given order id, or 404 when no payment exists."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Payment found"),
			@ApiResponse(responseCode = "400", description = "Invalid order id"),
			@ApiResponse(responseCode = "404", description = "Payment not found")
	})
	public ResponseEntity<PaymentResponse> getByOrderId(
			@Parameter(description = "Order identifier.", example = "9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f")
			@PathVariable UUID orderId) {
		log.info("Looking up payment for order={}", orderId);
		return ResponseEntity.ok(PaymentResponse.from(getPaymentUseCase.execute(orderId)));
	}
}
