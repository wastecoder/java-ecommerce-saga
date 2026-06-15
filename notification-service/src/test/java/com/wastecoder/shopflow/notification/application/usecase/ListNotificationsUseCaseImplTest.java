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
class ListNotificationsUseCaseImplTest {

	@Mock
	private NotificationRepository repository;

	@Test
	@DisplayName("Given recorded notifications, when listing, then the repository result is returned")
	void list_returnsRepositoryResult() {
		List<Notification> notifications = List.of(NotificationMother.aConfirmedNotification(),
				NotificationMother.aCreatedNotification());
		when(repository.findAll()).thenReturn(notifications);

		assertThat(new ListNotificationsUseCaseImpl(repository).execute()).isEqualTo(notifications);
	}
}
