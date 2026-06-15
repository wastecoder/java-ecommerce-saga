package com.wastecoder.shopflow.payment.adapter.messaging.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastecoder.shopflow.payment.adapter.messaging.Consumers;
import com.wastecoder.shopflow.payment.adapter.messaging.EventEnvelope;
import com.wastecoder.shopflow.payment.adapter.messaging.MessageType;
import com.wastecoder.shopflow.payment.application.port.in.AuthorizePaymentUseCase;
import com.wastecoder.shopflow.payment.application.port.in.RefundPaymentUseCase;
import com.wastecoder.shopflow.payment.application.port.out.ProcessedMessageRepository;
import com.wastecoder.shopflow.payment.application.viewmodel.PaymentCommand;
import com.wastecoder.shopflow.payment.testsupport.mother.PaymentCommandMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCommandListenerTest {

	@Mock
	private AuthorizePaymentUseCase authorizePaymentUseCase;

	@Mock
	private RefundPaymentUseCase refundPaymentUseCase;

	@Mock
	private ProcessedMessageRepository processedMessages;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private PaymentCommandListener listener() {
		// Real idempotency guard over a mocked ledger: exists==false (default) passes through to routing.
		return new PaymentCommandListener(authorizePaymentUseCase, refundPaymentUseCase, objectMapper,
				new IdempotentMessageProcessor(processedMessages));
	}

	private static EventEnvelope envelope(String type, Object payload) {
		return new EventEnvelope(UUID.randomUUID(), type, PaymentCommandMother.ORDER_ID.toString(), Instant.now(),
				payload);
	}

	@Test
	@DisplayName("Given a ProcessPayment command, when consumed, then the Map payload is converted to a PaymentCommand and AuthorizePayment is invoked")
	void routesAndConvertsProcess() {
		Map<String, Object> payload = PaymentCommandMother.asPayloadMap(PaymentCommandMother.aProcessCommand());

		listener().onMessage(envelope(MessageType.PROCESS_PAYMENT, payload));

		ArgumentCaptor<PaymentCommand> captor = ArgumentCaptor.forClass(PaymentCommand.class);
		verify(authorizePaymentUseCase).execute(captor.capture());
		assertThat(captor.getValue().orderId()).isEqualTo(PaymentCommandMother.ORDER_ID);
		assertThat(captor.getValue().customerId()).isEqualTo(PaymentCommandMother.CUSTOMER_ID);
		assertThat(captor.getValue().amount()).isEqualByComparingTo("100.00");
		verifyNoInteractions(refundPaymentUseCase);
	}

	@Test
	@DisplayName("Given a RefundPayment command, when consumed, then the converted PaymentCommand is passed to RefundPayment")
	void routesAndConvertsRefund() {
		Map<String, Object> payload = PaymentCommandMother.asPayloadMap(PaymentCommandMother.aRefundCommand());

		listener().onMessage(envelope(MessageType.REFUND_PAYMENT, payload));

		ArgumentCaptor<PaymentCommand> captor = ArgumentCaptor.forClass(PaymentCommand.class);
		verify(refundPaymentUseCase).execute(captor.capture());
		assertThat(captor.getValue().orderId()).isEqualTo(PaymentCommandMother.ORDER_ID);
		verifyNoInteractions(authorizePaymentUseCase);
	}

	@Test
	@DisplayName("Given an unhandled command type, when consumed, then neither use case is invoked")
	void ignoresUnknownType() {
		listener().onMessage(envelope("SomethingElse", Map.of()));

		verifyNoInteractions(authorizePaymentUseCase, refundPaymentUseCase);
	}

	@Test
	@DisplayName("Given a command already processed by this consumer, when consumed again, then it is skipped and neither use case is invoked")
	void duplicateEvent_isSkipped() {
		when(processedMessages.existsByEventIdAndConsumer(any(), eq(Consumers.PAYMENT_COMMAND))).thenReturn(true);
		Map<String, Object> payload = PaymentCommandMother.asPayloadMap(PaymentCommandMother.aProcessCommand());

		listener().onMessage(envelope(MessageType.PROCESS_PAYMENT, payload));

		verifyNoInteractions(authorizePaymentUseCase, refundPaymentUseCase);
	}
}
