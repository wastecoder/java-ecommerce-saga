package com.wastecoder.shopflow.order.adapter.web.handler;

import com.wastecoder.shopflow.order.domain.exception.DomainException;
import com.wastecoder.shopflow.order.domain.exception.OrderNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String GENERIC_INTERNAL_ERROR_DETAIL = "An unexpected error occurred";

	private final String baseUri;

	public GlobalExceptionHandler(@Value("${problem-details.base-uri}") String baseUri) {
		this.baseUri = baseUri;
	}

	@ExceptionHandler(OrderNotFoundException.class)
	public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
		log.warn("Order not found: {}", ex.orderId());
		ProblemDetail problem = problem(ProblemType.ORDER_NOT_FOUND, ex.getMessage());
		problem.setProperty("orderId", ex.orderId());
		return problem;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		List<Map<String, String>> errors = ex.getConstraintViolations().stream()
				.map(this::toFieldError)
				.toList();
		log.warn("Bean Validation failed in use case: {} violation(s)", errors.size());
		ProblemDetail problem = problem(
				ProblemType.VALIDATION_FAILED,
				"Validation failed: " + errors.size() + " error(s)");
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldError)
				.toList();
		log.warn("Request body validation failed: {} violation(s)", errors.size());
		ProblemDetail problem = problem(
				ProblemType.VALIDATION_FAILED,
				"Validation failed: " + errors.size() + " error(s)");
		problem.setProperty("errors", errors);
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

	private Map<String, String> toFieldError(ConstraintViolation<?> violation) {
		return Map.of(
				"field", violation.getPropertyPath().toString(),
				"message", violation.getMessage());
	}

	private Map<String, String> toFieldError(FieldError fieldError) {
		return Map.of(
				"field", fieldError.getField(),
				"message", fieldError.getDefaultMessage() == null ? "" : fieldError.getDefaultMessage());
	}
}
