package com.wastecoder.shopflow.order.adapter.web;

import com.wastecoder.shopflow.order.application.port.in.GetOrderUseCase;
import com.wastecoder.shopflow.order.application.port.in.PlaceOrderUseCase;
import com.wastecoder.shopflow.order.domain.exception.OrderNotFoundException;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.testsupport.mother.OrderMother;
import com.wastecoder.shopflow.order.testsupport.mother.PlaceOrderRequestPayloadMother;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlaceOrderUseCase placeOrderUseCase;

	@MockitoBean
	private GetOrderUseCase getOrderUseCase;

	@Test
	@DisplayName("Given a valid payload, when POST /orders, then it returns 201 with Location and the PENDING order")
	void place_returnsCreated() throws Exception {
		Order order = OrderMother.aPendingOrder();
		when(placeOrderUseCase.execute(any())).thenReturn(order);

		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(PlaceOrderRequestPayloadMother.aValidPayload()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/orders/" + OrderMother.ORDER_ID)))
				.andExpect(jsonPath("$.id").value(OrderMother.ORDER_ID.toString()))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.items[0].id").value(OrderMother.ITEM_ID.toString()))
				.andExpect(jsonPath("$.items[0].quantity").value(2));
	}

	@Test
	@DisplayName("Given an existing order, when GET /orders/{id}, then it returns 200 with the order")
	void getById_returnsOk() throws Exception {
		when(getOrderUseCase.execute(OrderMother.ORDER_ID)).thenReturn(OrderMother.aPendingOrder());

		mockMvc.perform(get("/orders/{id}", OrderMother.ORDER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(OrderMother.ORDER_ID.toString()))
				.andExpect(jsonPath("$.customerId").value(OrderMother.CUSTOMER_ID.toString()));
	}

	@Test
	@DisplayName("Given no order with the id, when GET /orders/{id}, then it returns 404 problem details")
	void getById_returnsNotFound() throws Exception {
		UUID missingId = UUID.fromString("99999999-9999-9999-9999-999999999999");
		when(getOrderUseCase.execute(missingId)).thenThrow(new OrderNotFoundException(missingId));

		mockMvc.perform(get("/orders/{id}", missingId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Order not found"))
				.andExpect(jsonPath("$.orderId").value(missingId.toString()));
	}

	@Test
	@DisplayName("Given a payload with no items, when POST /orders, then it returns 400 problem details")
	void place_returnsBadRequestWhenItemsEmpty() throws Exception {
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(PlaceOrderRequestPayloadMother.aPayloadWithoutItems()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"));
	}

	@Test
	@DisplayName("Given a non-UUID id, when GET /orders/{id}, then it returns 400 problem details")
	void getById_returnsBadRequestForInvalidId() throws Exception {
		mockMvc.perform(get("/orders/{id}", "not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request parameter"));
	}

	@Test
	@DisplayName("Given a use case that fails Bean Validation, when POST /orders, then it returns 400 problem details")
	void place_returnsBadRequestOnConstraintViolation() throws Exception {
		when(placeOrderUseCase.execute(any()))
				.thenThrow(new ConstraintViolationException("invalid", Collections.emptySet()));

		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(PlaceOrderRequestPayloadMother.aValidPayload()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"));
	}

	@Test
	@DisplayName("Given an unexpected failure, when POST /orders, then it returns 500 problem details")
	void place_returnsInternalServerErrorOnUnexpectedFailure() throws Exception {
		when(placeOrderUseCase.execute(any())).thenThrow(new RuntimeException("boom"));

		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(PlaceOrderRequestPayloadMother.aValidPayload()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.title").value("Internal server error"));
	}
}
