package com.wastecoder.shopflow.payment;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Shared Testcontainers for the integration suite. The containers are static singletons, started once and
 * reused across every Spring context.
 *
 * <p>Each integration test declares its own nested {@code @TestConfiguration}, so each gets a distinct,
 * cached {@code ApplicationContext}. With per-context {@code @Bean} containers, every context would boot its
 * own Kafka + PostgreSQL, and because cached contexts stay open for the whole run those containers
 * accumulate — on a resource-constrained CI runner that exhausts the host and the container readiness wait
 * times out ({@code LogMessageWaitStrategy} → {@code ContainerLaunchException}), failing context load.
 * Sharing one set keeps a single Kafka + PostgreSQL alive regardless of how many contexts the suite creates.
 *
 * <p>Image tags are pinned to the versions used in {@code docker-compose.yml} (reproducible; no {@code
 * :latest} drift) with a generous startup timeout for slower CI hosts. No test uses {@code @DirtiesContext}
 * and the suite stays well under the context-cache size, so no context closes mid-run and the singletons
 * live until JVM shutdown.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	private static final KafkaContainer KAFKA =
			new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.3.0"));

	private static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

	static {
		KAFKA.withStartupTimeout(Duration.ofMinutes(2));
		POSTGRES.withStartupTimeout(Duration.ofMinutes(2));
		KAFKA.start();
		POSTGRES.start();
	}

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return KAFKA;
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return POSTGRES;
	}

}
