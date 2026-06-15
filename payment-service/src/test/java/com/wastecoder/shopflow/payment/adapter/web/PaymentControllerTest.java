package com.wastecoder.shopflow.payment.adapter.web;

import com.wastecoder.shopflow.payment.application.port.in.GetPaymentUseCase;
import com.wastecoder.shopflow.payment.domain.exception.PaymentNotFoundException;
import com.wastecoder.shopflow.payment.testsupport.mother.PaymentMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetPaymentUseCase getPaymentUseCase;

	@Test
	@DisplayName("Given a payment for the order, when GET /payments/{orderId}, then it returns 200 with the payment")
	void getByOrderId_returnsOk() throws Exception {
		when(getPaymentUseCase.execute(PaymentMother.ORDER_ID)).thenReturn(PaymentMother.anAuthorizedPayment());

		mockMvc.perform(get("/payments/{orderId}", PaymentMother.ORDER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(PaymentMother.PAYMENT_ID.toString()))
				.andExpect(jsonPath("$.orderId").value(PaymentMother.ORDER_ID.toString()))
				.andExpect(jsonPath("$.status").value("AUTHORIZED"))
				.andExpect(jsonPath("$.providerRef").value(PaymentMother.PROVIDER_REF));
	}

	@Test
	@DisplayName("Given no payment for the order, when GET /payments/{orderId}, then it returns 404 problem details")
	void getByOrderId_returnsNotFound() throws Exception {
		when(getPaymentUseCase.execute(any())).thenThrow(new PaymentNotFoundException(PaymentMother.ORDER_ID));

		mockMvc.perform(get("/payments/{orderId}", PaymentMother.ORDER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Payment not found"))
				.andExpect(jsonPath("$.orderId").value(PaymentMother.ORDER_ID.toString()));
	}

	@Test
	@DisplayName("Given a non-UUID order id, when GET /payments/{orderId}, then it returns 400 problem details")
	void getByOrderId_returnsBadRequestForInvalidId() throws Exception {
		mockMvc.perform(get("/payments/{orderId}", "not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request parameter"));
	}

	@Test
	@DisplayName("Given an unexpected failure, when GET /payments/{orderId}, then it returns 500 problem details")
	void getByOrderId_returnsInternalServerErrorOnUnexpectedFailure() throws Exception {
		when(getPaymentUseCase.execute(any())).thenThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/payments/{orderId}", PaymentMother.ORDER_ID))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.title").value("Internal server error"));
	}
}
