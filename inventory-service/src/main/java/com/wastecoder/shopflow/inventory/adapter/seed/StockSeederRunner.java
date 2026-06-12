package com.wastecoder.shopflow.inventory.adapter.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.wastecoder.shopflow.inventory.adapter.seed.dto.SampleStockJson;
import com.wastecoder.shopflow.inventory.application.port.in.SeedStockUseCase;
import com.wastecoder.shopflow.inventory.application.viewmodel.SeedResult;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Component
@Profile("seed")
public class StockSeederRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(StockSeederRunner.class);
	private static final String SEED_RESOURCE_PATH = "seed/sample-stock.json";

	private final SeedStockUseCase seedStockUseCase;
	private final ObjectMapper objectMapper;

	public StockSeederRunner(SeedStockUseCase seedStockUseCase, ObjectMapper objectMapper) {
		this.seedStockUseCase = seedStockUseCase;
		this.objectMapper = objectMapper;
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Seed profile active — loading stock from classpath:{}", SEED_RESOURCE_PATH);

		List<SampleStockJson> samples;
		try (InputStream in = new ClassPathResource(SEED_RESOURCE_PATH).getInputStream()) {
			samples = objectMapper.readValue(in, new TypeReference<List<SampleStockJson>>() {
			});
		}

		List<StockItem> items = samples.stream()
				.map(sample -> new StockItem(UUID.fromString(sample.productId()), sample.available(), 0))
				.toList();

		SeedResult result = seedStockUseCase.execute(items);
		log.info("Seed finished: requested={}, inserted={}, skipped={}",
				result.requested(), result.inserted(), result.skipped());
	}
}
