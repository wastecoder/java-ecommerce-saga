package com.wastecoder.shopflow.payment.adapter.web.handler;

import com.wastecoder.shopflow.payment.domain.exception.DomainException;
import com.wastecoder.shopflow.payment.domain.exception.PaymentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String GENERIC_INTERNAL_ERROR_DETAIL = "An unexpected error occurred";

	private final String baseUri;

	public GlobalExceptionHandler(@Value("${problem-details.base-uri}") String baseUri) {
		this.baseUri = baseUri;
	}

	@ExceptionHandler(PaymentNotFoundException.class)
	public ProblemDetail handlePaymentNotFound(PaymentNotFoundException ex) {
		log.warn("Payment not found for order: {}", ex.orderId());
		ProblemDetail problem = problem(ProblemType.PAYMENT_NOT_FOUND, ex.getMessage());
		problem.setProperty("orderId", ex.orderId());
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
		log.warn("Invalid request parameter '{}': {}", ex.getName(), ex.getMessage());
		ProblemDetail problem = problem(ProblemType.INVALID_REQUEST_PARAMETER, ex.getMessage());
		problem.setProperty("parameter", ex.getName());
		return problem;
	}

	@ExceptionHandler(DomainException.class)
	public ProblemDetail handleDomainException(DomainException ex) {
		log.warn("Unhandled domain exception ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
		return problem(ProblemType.DOMAIN_ERROR, ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return problem(ProblemType.INTERNAL_SERVER_ERROR, GENERIC_INTERNAL_ERROR_DETAIL);
	}

	private ProblemDetail problem(ProblemType type, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.status(), detail);
		problem.setType(type.toUri(baseUri));
		problem.setTitle(type.title());
		return problem;
	}
}
