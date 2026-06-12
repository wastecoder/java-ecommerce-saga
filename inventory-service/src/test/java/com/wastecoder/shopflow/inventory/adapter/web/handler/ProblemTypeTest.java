package com.wastecoder.shopflow.inventory.adapter.web.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemTypeTest {

	@Test
	@DisplayName("Given a base uri without a trailing slash, when building the type uri, then the slug is appended")
	void toUri_withoutTrailingSlash() {
		assertThat(ProblemType.STOCK_ITEM_NOT_FOUND.toUri("https://wastecoder.com/problems"))
				.hasToString("https://wastecoder.com/problems/stock-item-not-found");
	}

	@Test
	@DisplayName("Given a base uri with a trailing slash, when building the type uri, then the slash is normalised")
	void toUri_withTrailingSlash() {
		assertThat(ProblemType.STOCK_ITEM_NOT_FOUND.toUri("https://wastecoder.com/problems/"))
				.hasToString("https://wastecoder.com/problems/stock-item-not-found");
	}

	@Test
	@DisplayName("Given a problem type, when reading its attributes, then slug, title and status are exposed")
	void exposesAttributes() {
		assertThat(ProblemType.STOCK_ITEM_NOT_FOUND.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(ProblemType.VALIDATION_FAILED.title()).isEqualTo("Validation failed");
		assertThat(ProblemType.DOMAIN_ERROR.slug()).isEqualTo("domain-error");
	}
}
