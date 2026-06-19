package com.wastecoder.shopflow.inventory.adapter.web;

import com.wastecoder.shopflow.inventory.adapter.config.SecurityConfig;
import com.wastecoder.shopflow.inventory.application.port.in.GetStockUseCase;
import com.wastecoder.shopflow.inventory.application.port.in.ListStockUseCase;
import com.wastecoder.shopflow.inventory.domain.exception.StockItemNotFoundException;
import com.wastecoder.shopflow.inventory.testsupport.mother.StockItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@Import(SecurityConfig.class)
class StockControllerTest {

	private static final RequestPostProcessor ADMIN = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	private static final RequestPostProcessor CUSTOMER = jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListStockUseCase listStockUseCase;

	@MockitoBean
	private GetStockUseCase getStockUseCase;

	// Required so the oauth2ResourceServer().jwt() filter wires without reaching a real Keycloak; the jwt()
	// post-processor authenticates requests directly, so this decoder is never actually invoked.
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	@DisplayName("Given an ADMIN token and stock exists, when GET /stock, then it returns 200 with the list")
	void list_returnsOk() throws Exception {
		when(listStockUseCase.execute()).thenReturn(List.of(StockItemMother.aStockItem()));

		mockMvc.perform(get("/stock").with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(StockItemMother.PRODUCT_ID.toString()))
				.andExpect(jsonPath("$[0].available").value(100))
				.andExpect(jsonPath("$[0].reserved").value(0));
	}

	@Test
	@DisplayName("Given no token, when GET /stock, then it returns 401 Unauthorized")
	void list_returnsUnauthorizedWithoutToken() throws Exception {
		mockMvc.perform(get("/stock"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Given a token without the ADMIN role, when GET /stock, then it returns 403 Forbidden")
	void list_returnsForbiddenWithoutAdminRole() throws Exception {
		mockMvc.perform(get("/stock").with(CUSTOMER))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Given an ADMIN token and a product with stock, when GET /stock/{id}, then it returns 200 with the stock")
	void getByProductId_returnsOk() throws Exception {
		when(getStockUseCase.execute(StockItemMother.PRODUCT_ID)).thenReturn(StockItemMother.aStockItem());

		mockMvc.perform(get("/stock/{productId}", StockItemMother.PRODUCT_ID).with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(StockItemMother.PRODUCT_ID.toString()))
				.andExpect(jsonPath("$.available").value(100));
	}

	@Test
	@DisplayName("Given an ADMIN token and no stock for the product, when GET /stock/{id}, then it returns 404 problem details")
	void getByProductId_returnsNotFound() throws Exception {
		when(getStockUseCase.execute(any()))
				.thenThrow(new StockItemNotFoundException(StockItemMother.PRODUCT_ID));

		mockMvc.perform(get("/stock/{productId}", StockItemMother.PRODUCT_ID).with(ADMIN))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Stock item not found"))
				.andExpect(jsonPath("$.productId").value(StockItemMother.PRODUCT_ID.toString()));
	}

	@Test
	@DisplayName("Given an ADMIN token and a non-UUID id, when GET /stock/{id}, then it returns 400 problem details")
	void getByProductId_returnsBadRequestForInvalidId() throws Exception {
		mockMvc.perform(get("/stock/{productId}", "not-a-uuid").with(ADMIN))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request parameter"));
	}

	@Test
	@DisplayName("Given an ADMIN token and an unexpected failure, when GET /stock, then it returns 500 problem details")
	void list_returnsInternalServerErrorOnUnexpectedFailure() throws Exception {
		when(listStockUseCase.execute()).thenThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/stock").with(ADMIN))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.title").value("Internal server error"));
	}
}
