package com.wastecoder.shopflow.inventory.adapter.web;

import com.wastecoder.shopflow.inventory.adapter.web.dto.response.StockResponse;
import com.wastecoder.shopflow.inventory.application.port.in.GetStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.in.ListStockUseCase;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/stock")
@Tag(name = "Stock Endpoints", description = "Operations to query product stock levels.")
public class StockController {

	private static final Logger log = LoggerFactory.getLogger(StockController.class);

	private final ListStockUseCase listStockUseCase;
	private final GetStockUseCase getStockUseCase;

	public StockController(ListStockUseCase listStockUseCase, GetStockUseCase getStockUseCase) {
		this.listStockUseCase = listStockUseCase;
		this.getStockUseCase = getStockUseCase;
	}

	@GetMapping
	@Operation(summary = "List all stock items", description = "Returns the current stock level of every product.")
	@ApiResponse(responseCode = "200", description = "Stock listed")
	public ResponseEntity<List<StockResponse>> list() {
		log.info("Listing stock");
		List<StockResponse> stock = listStockUseCase.execute().stream()
				.map(StockResponse::from)
				.toList();
		return ResponseEntity.ok(stock);
	}

	@GetMapping("/{productId}")
	@Operation(
			summary = "Retrieve stock for a product",
			description = "Returns the stock level for the given product id, or 404 when no stock exists."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Stock found"),
			@ApiResponse(responseCode = "400", description = "Invalid product id"),
			@ApiResponse(responseCode = "404", description = "Stock not found")
	})
	public ResponseEntity<StockResponse> getByProductId(@PathVariable UUID productId) {
		log.info("Looking up stock for product={}", productId);
		return ResponseEntity.ok(StockResponse.from(getStockUseCase.execute(productId)));
	}
}
