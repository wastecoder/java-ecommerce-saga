package com.wastecoder.shopflow.inventory.adapter.seed;

import com.wastecoder.shopflow.inventory.TestcontainersConfiguration;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("seed")
class StockSeederIntegrationTest {

	private static final UUID SEEDED_PRODUCT_ID = UUID.fromString("a1111111-1111-1111-1111-111111111111");

	@Autowired
	private StockRepository repository;

	@Test
	@DisplayName("Given the seed profile is active, when the application starts, then sample stock is seeded")
	void seedRunner_populatesStock() {
		assertThat(repository.findByProductId(SEEDED_PRODUCT_ID))
				.isPresent()
				.get()
				.satisfies(item -> assertThat(item.available()).isEqualTo(100));
		assertThat(repository.findAll()).hasSizeGreaterThanOrEqualTo(4);
	}
}
