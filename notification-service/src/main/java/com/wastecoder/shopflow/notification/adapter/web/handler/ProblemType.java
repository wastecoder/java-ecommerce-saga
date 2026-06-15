package com.wastecoder.shopflow.notification.adapter.web.handler;

import org.springframework.http.HttpStatus;

import java.net.URI;

public enum ProblemType {

	INVALID_REQUEST_PARAMETER("invalid-request-parameter", "Invalid request parameter", HttpStatus.BAD_REQUEST),
	INTERNAL_SERVER_ERROR("internal-server-error", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String slug;
	private final String title;
	private final HttpStatus status;

	ProblemType(String slug, String title, HttpStatus status) {
		this.slug = slug;
		this.title = title;
		this.status = status;
	}

	public String slug() {
		return slug;
	}

	public String title() {
		return title;
	}

	public HttpStatus status() {
		return status;
	}

	public URI toUri(String baseUri) {
		String normalized = baseUri.endsWith("/") ? baseUri.substring(0, baseUri.length() - 1) : baseUri;
		return URI.create(normalized + "/" + slug);
	}
}
