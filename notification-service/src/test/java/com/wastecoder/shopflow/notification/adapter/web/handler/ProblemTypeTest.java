package com.wastecoder.shopflow.notification.adapter.web.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemTypeTest {

	@Test
	@DisplayName("Given a base uri without a trailing slash, when building the type uri, then the slug is appended")
	void toUri_withoutTrailingSlash() {
		assertThat(ProblemType.INVALID_REQUEST_PARAMETER.toUri("https://wastecoder.com/problems"))
				.hasToString("https://wastecoder.com/problems/invalid-request-parameter");
	}

	@Test
	@DisplayName("Given a base uri with a trailing slash, when building the type uri, then the slash is normalised")
	void toUri_withTrailingSlash() {
		assertThat(ProblemType.INVALID_REQUEST_PARAMETER.toUri("https://wastecoder.com/problems/"))
				.hasToString("https://wastecoder.com/problems/invalid-request-parameter");
	}

	@Test
	@DisplayName("Given a problem type, when reading its attributes, then slug, title and status are exposed")
	void exposesAttributes() {
		assertThat(ProblemType.INVALID_REQUEST_PARAMETER.status()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(ProblemType.INTERNAL_SERVER_ERROR.title()).isEqualTo("Internal server error");
		assertThat(ProblemType.INTERNAL_SERVER_ERROR.slug()).isEqualTo("internal-server-error");
	}
}
