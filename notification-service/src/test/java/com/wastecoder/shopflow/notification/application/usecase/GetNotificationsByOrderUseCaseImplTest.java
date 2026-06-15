package com.wastecoder.shopflow.notification.application.usecase;

import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.testsupport.mother.NotificationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetNotificationsByOrderUseCaseImplTest {

	@Mock
	private NotificationRepository repository;

	@Test
	@DisplayName("Given notifications for an order, when querying by order id, then the repository result is returned")
	void getByOrder_returnsRepositoryResult() {
		List<Notification> notifications = List.of(NotificationMother.aConfirmedNotification());
		when(repository.findByOrderId(NotificationMother.ORDER_ID)).thenReturn(notifications);

		assertThat(new GetNotificationsByOrderUseCaseImpl(repository).execute(NotificationMother.ORDER_ID))
				.isEqualTo(notifications);
	}
}
