package com.wastecoder.shopflow.order.adapter.web;

import com.wastecoder.shopflow.order.adapter.web.dto.request.PlaceOrderRequest;
import com.wastecoder.shopflow.order.adapter.web.dto.response.OrderResponse;
import com.wastecoder.shopflow.order.application.port.in.GetOrderUseCase;
import com.wastecoder.shopflow.order.application.port.in.PlaceOrderUseCase;
import com.wastecoder.shopflow.order.domain.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Order Endpoints", description = "Operations to place and retrieve customer orders.")
public class OrderController {

	private static final Logger log = LoggerFactory.getLogger(OrderController.class);

	private final PlaceOrderUseCase placeOrderUseCase;
	private final GetOrderUseCase getOrderUseCase;

	public OrderController(PlaceOrderUseCase placeOrderUseCase, GetOrderUseCase getOrderUseCase) {
		this.placeOrderUseCase = placeOrderUseCase;
		this.getOrderUseCase = getOrderUseCase;
	}

	@PostMapping
	@Operation(
			summary = "Place a new order",
			description = "Creates an order in PENDING status and returns 201 Created with the Location header."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Order created"),
			@ApiResponse(responseCode = "400", description = "Invalid payload")
	})
	public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
		log.info("Placing order for customer={} items={}", request.customerId(), request.items().size());
		Order order = placeOrderUseCase.execute(request.toCommand());

		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(order.id())
				.toUri();

		return ResponseEntity.created(location).body(OrderResponse.from(order));
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Retrieve an order by id",
			description = "Returns the order matching the given id, or 404 when no order exists with that id."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Order found"),
			@ApiResponse(responseCode = "400", description = "Invalid id"),
			@ApiResponse(responseCode = "404", description = "Order not found")
	})
	public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
		log.info("Looking up order id={}", id);
		Order order = getOrderUseCase.execute(id);
		return ResponseEntity.ok(OrderResponse.from(order));
	}
}
