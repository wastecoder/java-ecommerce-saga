package com.wastecoder.shopflow.notification.application.usecase;

import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.application.viewmodel.NotificationCommand;
import com.wastecoder.shopflow.notification.domain.model.Notification;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import com.wastecoder.shopflow.notification.testsupport.mother.NotificationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordNotificationUseCaseImplTest {

	@Mock
	private NotificationRepository repository;

	private RecordNotificationUseCaseImpl useCase() {
		return new RecordNotificationUseCaseImpl(repository);
	}

	@Test
	@DisplayName("Given an order-confirmed command, when executed, then a confirmed notification is saved with the rendered message")
	void recordsConfirmedNotification() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		NotificationCommand command = new NotificationCommand(NotificationMother.ORDER_ID,
				NotificationMother.CUSTOMER_ID, NotificationType.ORDER_CONFIRMED, NotificationMother.TOTAL_AMOUNT);

		useCase().execute(command);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(repository).save(captor.capture());
		Notification saved = captor.getValue();
		assertThat(saved.id()).isNotNull();
		assertThat(saved.orderId()).isEqualTo(NotificationMother.ORDER_ID);
		assertThat(saved.customerId()).isEqualTo(NotificationMother.CUSTOMER_ID);
		assertThat(saved.type()).isEqualTo(NotificationType.ORDER_CONFIRMED);
		assertThat(saved.message())
				.isEqualTo(NotificationType.ORDER_CONFIRMED.message(NotificationMother.ORDER_ID,
						NotificationMother.TOTAL_AMOUNT));
		assertThat(saved.createdAt()).isNotNull();
	}

	@Test
	@DisplayName("Given an order-cancelled command, when executed, then a cancelled notification is saved")
	void recordsCancelledNotification() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		NotificationCommand command = new NotificationCommand(NotificationMother.ORDER_ID,
				NotificationMother.CUSTOMER_ID, NotificationType.ORDER_CANCELLED, NotificationMother.TOTAL_AMOUNT);

		useCase().execute(command);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().type()).isEqualTo(NotificationType.ORDER_CANCELLED);
		assertThat(captor.getValue().message()).contains("cancelled");
	}
}
