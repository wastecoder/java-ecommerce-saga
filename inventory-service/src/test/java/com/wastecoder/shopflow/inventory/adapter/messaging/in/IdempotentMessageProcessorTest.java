package com.wastecoder.shopflow.inventory.adapter.messaging.in;

import com.wastecoder.shopflow.inventory.adapter.messaging.Consumers;
import com.wastecoder.shopflow.inventory.application.port.out.ProcessedMessage;
import com.wastecoder.shopflow.inventory.application.port.out.ProcessedMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentMessageProcessorTest {

	private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final String CONSUMER = Consumers.STOCK_COMMAND;

	@Mock
	private ProcessedMessageRepository repository;

	@InjectMocks
	private IdempotentMessageProcessor processor;

	@Test
	@DisplayName("Given a new event, when processed, then the handler runs and the marker is saved afterwards")
	void newEvent_runsHandlerThenSaves() {
		Runnable handler = mock(Runnable.class);

		processor.processOnce(EVENT_ID, CONSUMER, handler);

		InOrder inOrder = inOrder(handler, repository);
		inOrder.verify(handler).run();
		ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
		inOrder.verify(repository).save(captor.capture());
		assertThat(captor.getValue().eventId()).isEqualTo(EVENT_ID);
		assertThat(captor.getValue().consumer()).isEqualTo(CONSUMER);
		assertThat(captor.getValue().processedAt()).isNotNull();
	}

	@Test
	@DisplayName("Given an already-processed event, when processed, then the handler does not run and nothing is saved")
	void duplicate_skipsHandlerAndSave() {
		when(repository.existsByEventIdAndConsumer(EVENT_ID, CONSUMER)).thenReturn(true);
		Runnable handler = mock(Runnable.class);

		processor.processOnce(EVENT_ID, CONSUMER, handler);

		verifyNoInteractions(handler);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Given a handler that throws, when processed, then the exception propagates and no marker is saved")
	void handlerThrows_propagatesAndDoesNotSave() {
		Runnable handler = () -> {
			throw new IllegalStateException("boom");
		};

		assertThatThrownBy(() -> processor.processOnce(EVENT_ID, CONSUMER, handler))
				.isInstanceOf(IllegalStateException.class);

		verify(repository, never()).save(any());
	}
}
