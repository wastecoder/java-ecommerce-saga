package com.wastecoder.shopflow.notification.adapter.web.handler;

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

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
		log.warn("Invalid request parameter '{}': {}", ex.getName(), ex.getMessage());
		ProblemDetail problem = problem(ProblemType.INVALID_REQUEST_PARAMETER, ex.getMessage());
		problem.setProperty("parameter", ex.getName());
		return problem;
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
