package com.wastecoder.shopflow.notification.adapter.web;

import com.wastecoder.shopflow.notification.adapter.config.SecurityConfig;
import com.wastecoder.shopflow.notification.application.port.in.GetNotificationsByOrderUseCase;
import com.wastecoder.shopflow.notification.application.port.in.ListNotificationsUseCase;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.testsupport.mother.NotificationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListNotificationsUseCase listNotificationsUseCase;

	@MockitoBean
	private GetNotificationsByOrderUseCase getNotificationsByOrderUseCase;

	// Required so the oauth2ResourceServer().jwt() filter wires without reaching a real Keycloak; the jwt()
	// post-processor authenticates requests directly, so this decoder is never actually invoked.
	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	@DisplayName("Given an authenticated token and recorded notifications, when GET /notifications, then it returns 200 with the list")
	void list_returnsOk() throws Exception {
		Notification notification = NotificationMother.aConfirmedNotification();
		when(listNotificationsUseCase.execute()).thenReturn(List.of(notification));

		mockMvc.perform(get("/notifications").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(NotificationMother.NOTIFICATION_ID.toString()))
				.andExpect(jsonPath("$[0].orderId").value(NotificationMother.ORDER_ID.toString()))
				.andExpect(jsonPath("$[0].type").value("ORDER_CONFIRMED"))
				.andExpect(jsonPath("$[0].message").value(notification.message()));
	}

	@Test
	@DisplayName("Given no token, when GET /notifications, then it returns 401 Unauthorized")
	void list_returnsUnauthorizedWithoutToken() throws Exception {
		mockMvc.perform(get("/notifications"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Given an authenticated token and notifications for an order, when GET /notifications/{orderId}, then it returns 200 with the list")
	void getByOrderId_returnsOk() throws Exception {
		Notification notification = NotificationMother.aConfirmedNotification();
		when(getNotificationsByOrderUseCase.execute(NotificationMother.ORDER_ID)).thenReturn(List.of(notification));

		mockMvc.perform(get("/notifications/{orderId}", NotificationMother.ORDER_ID).with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].orderId").value(NotificationMother.ORDER_ID.toString()))
				.andExpect(jsonPath("$[0].type").value("ORDER_CONFIRMED"));
	}

	@Test
	@DisplayName("Given an authenticated token and no notifications for an order, when GET /notifications/{orderId}, then it returns 200 with an empty list")
	void getByOrderId_returnsEmptyList() throws Exception {
		when(getNotificationsByOrderUseCase.execute(any())).thenReturn(List.of());

		mockMvc.perform(get("/notifications/{orderId}", NotificationMother.ORDER_ID).with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Given an authenticated token and a non-UUID order id, when GET /notifications/{orderId}, then it returns 400 problem details")
	void getByOrderId_returnsBadRequestForInvalidId() throws Exception {
		mockMvc.perform(get("/notifications/{orderId}", "not-a-uuid").with(jwt()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request parameter"));
	}

	@Test
	@DisplayName("Given an authenticated token and an unexpected failure, when GET /notifications, then it returns 500 problem details")
	void list_returnsInternalServerErrorOnUnexpectedFailure() throws Exception {
		when(listNotificationsUseCase.execute()).thenThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/notifications").with(jwt()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.title").value("Internal server error"));
	}
}
