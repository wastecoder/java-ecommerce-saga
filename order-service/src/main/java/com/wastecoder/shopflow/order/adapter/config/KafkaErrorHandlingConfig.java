package com.wastecoder.shopflow.order.adapter.config;

import com.wastecoder.shopflow.order.adapter.messaging.EventEnvelope;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Blocking retry + dead-letter wiring for the SAGA reply consumers. Spring Boot auto-associates this single
 * {@link DefaultErrorHandler} bean with the auto-configured listener container factory, so no custom factory
 * is needed. After the fixed retries — or immediately, for non-retryable errors — the record is published to
 * {@code <topic>.DLT}.
 */
@Configuration
public class KafkaErrorHandlingConfig {

	@Bean
	DefaultErrorHandler kafkaErrorHandler(ProducerFactory<Object, Object> producerFactory) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
				deadLetterTemplate(producerFactory),
				// Spring Kafka 4.0 changed the default suffix to "-dlt"; force the documented ".DLT" contract,
				// routing to the same partition number as the source record.
				(failedRecord, exception) -> new TopicPartition(failedRecord.topic() + ".DLT", failedRecord.partition()));
		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
		// Malformed orderId/payload is deterministic — retries never help, so route straight to the DLT.
		handler.addNotRetryableExceptions(IllegalArgumentException.class);
		return handler;
	}

	/**
	 * Dedicated DLT template, deliberately NOT exposed as a bean so Boot's auto-configured
	 * {@code KafkaTemplate}/{@code ProducerFactory} (which the publishers rely on) are preserved. It reuses
	 * the auto producer config (correct broker address, including under Testcontainers) and routes the value
	 * by type: a normal {@link EventEnvelope} is written as JSON without type headers, while a poison-pill
	 * value (surfaced by {@code ErrorHandlingDeserializer} as raw {@code byte[]}) passes through untouched.
	 */
	private KafkaTemplate<String, Object> deadLetterTemplate(ProducerFactory<Object, Object> base) {
		Map<String, Object> configs = new HashMap<>(base.getConfigurationProperties());
		// The delegate JacksonJsonSerializer is configured programmatically (noTypeInfo); drop the inherited
		// spring.json.* producer properties so it is not configured both ways, which Spring Kafka forbids.
		configs.keySet().removeIf(key -> key.startsWith("spring.json."));
		ProducerFactory<String, Object> deadLetterFactory = new DefaultKafkaProducerFactory<>(
				configs,
				new StringSerializer(),
				new DelegatingByTypeSerializer(Map.<Class<?>, Serializer<?>>of(
						byte[].class, new ByteArraySerializer(),
						EventEnvelope.class, new JacksonJsonSerializer<>().noTypeInfo())));
		return new KafkaTemplate<>(deadLetterFactory);
	}
}
