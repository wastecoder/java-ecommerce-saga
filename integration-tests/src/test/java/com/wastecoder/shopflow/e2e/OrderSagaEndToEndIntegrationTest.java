package com.wastecoder.shopflow.e2e;

import com.wastecoder.shopflow.inventory.InventoryServiceApplication;
import com.wastecoder.shopflow.inventory.application.port.out.StockRepository;
import com.wastecoder.shopflow.inventory.domain.model.StockItem;
import com.wastecoder.shopflow.notification.NotificationServiceApplication;
import com.wastecoder.shopflow.notification.application.port.out.NotificationRepository;
import com.wastecoder.shopflow.notification.domain.model.NotificationType;
import com.wastecoder.shopflow.order.OrderServiceApplication;
import com.wastecoder.shopflow.payment.PaymentServiceApplication;
import com.wastecoder.shopflow.payment.application.port.out.PaymentRepository;
import com.wastecoder.shopflow.payment.domain.model.Payment;
import com.wastecoder.shopflow.payment.domain.model.PaymentStatus;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * True end-to-end test of the orchestrated saga: the four real services ({@code order}, {@code inventory},
 * {@code payment}, {@code notification}) run as independent Spring contexts in this JVM, talking to one
 * shared Kafka broker and one shared PostgreSQL (one database per service). Nothing is simulated — an order
 * is placed over REST and the whole flow is observed through each service's own repository.
 *
 * <p>Each {@code @SpringBootApplication} component-scans only its own (disjoint) package, so the four
 * contexts do not see each other's beans/entities. Classpath collisions are sidestepped per context:
 * {@code --spring.config.location} loads a service-specific config instead of the four merged
 * {@code application.yaml} files, and Flyway is disabled in favour of {@code ddl-auto=create-drop} (the four
 * services ship overlapping {@code db/migration} versions that cannot share a classpath). The per-service
 * Flyway schemas are already validated by each module's own integration tests; this test covers the
 * cross-service wiring.
 *
 * <p>Fixtures are built inline here (stock seeded via {@code new StockItem(...)} and the request body as a
 * {@code Map}) rather than with the Object Mother pattern: the services' Mothers live in their
 * {@code src/test} and are not reachable from another module, and inline arrange is acceptable for a
 * black-box E2E test driven over REST.
 */
class OrderSagaEndToEndIntegrationTest {

	private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration POLL_INTERVAL = Duration.ofMillis(250);
	private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {
	};

	private static KafkaContainer kafka;
	private static PostgreSQLContainer postgres;

	private static ConfigurableApplicationContext orderContext;
	private static ConfigurableApplicationContext inventoryContext;
	private static ConfigurableApplicationContext paymentContext;
	private static ConfigurableApplicationContext notificationContext;

	private static String orderBaseUrl;
	private static RestClient rest;

	@BeforeAll
	static void startInfrastructureAndServices() throws Exception {
		kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
		postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
		kafka.start();
		postgres.start();

		createDatabases("orderdb", "inventorydb", "paymentdb", "notificationdb");

		String bootstrap = kafka.getBootstrapServers();
		// Pre-create every topic with the services' partition count BEFORE any context starts. Otherwise the
		// first service to subscribe to a topic auto-creates it with the broker default (1 partition); a later
		// service's KafkaAdmin then bumps it to 3, but the already-subscribed consumer is not reassigned the
		// new partitions in time, so keyed messages land on partitions nobody is consuming and the saga stalls.
		createTopics(bootstrap);

		inventoryContext = boot(InventoryServiceApplication.class, "inventory", "inventorydb", bootstrap, null);
		paymentContext = boot(PaymentServiceApplication.class, "payment", "paymentdb", bootstrap, null);
		notificationContext = boot(NotificationServiceApplication.class, "notification", "notificationdb", bootstrap, null);
		orderContext = boot(OrderServiceApplication.class, "order", "orderdb", bootstrap, "0");

		int port = ((WebServerApplicationContext) orderContext).getWebServer().getPort();
		orderBaseUrl = "http://localhost:" + port;
		rest = RestClient.create();
	}

	@AfterAll
	static void stopServicesAndInfrastructure() {
		close(orderContext);
		close(paymentContext);
		close(inventoryContext);
		close(notificationContext);
		if (postgres != null) {
			postgres.stop();
		}
		if (kafka != null) {
			kafka.stop();
		}
	}

	@Test
	@DisplayName("Given stock and an affordable order, when it is placed, then stock is reserved, payment authorized and the order CONFIRMED with a notification")
	void happyPath_confirmsOrder() {
		UUID productId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		stockRepository().save(new StockItem(productId, 100, 0));

		UUID orderId = placeOrder(customerId, productId, 2, new BigDecimal("100.00")); // total 200 < 1000

		awaitUntil(() -> "CONFIRMED".equals(orderStatus(orderId)), "order " + orderId + " to be CONFIRMED");

		Optional<Payment> payment = paymentRepository().findByOrderId(orderId);
		assertThat(payment).isPresent();
		assertThat(payment.get().status()).isEqualTo(PaymentStatus.AUTHORIZED);

		StockItem stock = stockRepository().findByProductId(productId).orElseThrow();
		assertThat(stock.available()).isEqualTo(98);
		assertThat(stock.reserved()).isEqualTo(2);

		awaitUntil(() -> hasNotification(orderId, NotificationType.ORDER_CONFIRMED),
				"an ORDER_CONFIRMED notification for order " + orderId);
	}

	@Test
	@DisplayName("Given stock but an order above the PSP threshold, when it is placed, then payment fails, stock is released (compensation) and the order is CANCELLED with a notification")
	void paymentDeclined_cancelsAndCompensates() {
		UUID productId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		stockRepository().save(new StockItem(productId, 100, 0));

		UUID orderId = placeOrder(customerId, productId, 6, new BigDecimal("200.00")); // total 1200 >= 1000

		awaitUntil(() -> "CANCELLED".equals(orderStatus(orderId)), "order " + orderId + " to be CANCELLED");

		Optional<Payment> payment = paymentRepository().findByOrderId(orderId);
		assertThat(payment).isPresent();
		assertThat(payment.get().status()).isEqualTo(PaymentStatus.FAILED);

		// ReleaseStock is emitted as compensation after the order is cancelled, so the restock is asynchronous.
		awaitUntil(() -> {
			StockItem stock = stockRepository().findByProductId(productId).orElseThrow();
			return stock.available() == 100 && stock.reserved() == 0;
		}, "stock to be released back for product " + productId);

		awaitUntil(() -> hasNotification(orderId, NotificationType.ORDER_CANCELLED),
				"an ORDER_CANCELLED notification for order " + orderId);
	}

	@Test
	@DisplayName("Given insufficient stock, when an order is placed, then it is REJECTED with no payment, stock untouched and a notification")
	void insufficientStock_rejectsOrder() {
		UUID productId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		stockRepository().save(new StockItem(productId, 1, 0));

		UUID orderId = placeOrder(customerId, productId, 5, new BigDecimal("100.00")); // wants 5, only 1 available

		awaitUntil(() -> "REJECTED".equals(orderStatus(orderId)), "order " + orderId + " to be REJECTED");

		awaitUntil(() -> hasNotification(orderId, NotificationType.ORDER_REJECTED),
				"an ORDER_REJECTED notification for order " + orderId);

		assertThat(paymentRepository().findByOrderId(orderId)).isEmpty();

		StockItem stock = stockRepository().findByProductId(productId).orElseThrow();
		assertThat(stock.available()).isEqualTo(1);
		assertThat(stock.reserved()).isZero();
	}

	private UUID placeOrder(UUID customerId, UUID productId, int quantity, BigDecimal unitPrice) {
		Map<String, Object> body = Map.of(
				"customerId", customerId.toString(),
				"items", List.of(Map.of(
						"productId", productId.toString(),
						"quantity", quantity,
						"unitPrice", unitPrice)));

		Map<String, Object> response = rest.post()
				.uri(orderBaseUrl + "/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(JSON_MAP);

		return UUID.fromString((String) response.get("id"));
	}

	private String orderStatus(UUID orderId) {
		Map<String, Object> response = rest.get()
				.uri(orderBaseUrl + "/orders/" + orderId)
				.retrieve()
				.body(JSON_MAP);
		return (String) response.get("status");
	}

	private static boolean hasNotification(UUID orderId, NotificationType type) {
		return notificationRepository().findByOrderId(orderId).stream()
				.anyMatch(notification -> notification.type() == type);
	}

	private static StockRepository stockRepository() {
		return inventoryContext.getBean(StockRepository.class);
	}

	private static PaymentRepository paymentRepository() {
		return paymentContext.getBean(PaymentRepository.class);
	}

	private static NotificationRepository notificationRepository() {
		return notificationContext.getBean(NotificationRepository.class);
	}

	private static ConfigurableApplicationContext boot(Class<?> applicationClass, String service, String database,
			String bootstrapServers, String serverPort) {
		String jdbcUrl = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
		List<String> args = new ArrayList<>(List.of(
				"--spring.config.location=classpath:/e2e/" + service + ".yaml",
				"--spring.datasource.url=" + jdbcUrl,
				"--spring.datasource.username=" + postgres.getUsername(),
				"--spring.datasource.password=" + postgres.getPassword(),
				"--spring.kafka.bootstrap-servers=" + bootstrapServers));
		if (serverPort != null) {
			args.add("--server.port=" + serverPort);
		}
		return new SpringApplicationBuilder(applicationClass).run(args.toArray(new String[0]));
	}

	private static void createTopics(String bootstrapServers) throws Exception {
		List<NewTopic> topics = List.of(
				new NewTopic("inventory.commands", 3, (short) 1),
				new NewTopic("inventory.events", 3, (short) 1),
				new NewTopic("payment.commands", 3, (short) 1),
				new NewTopic("payment.events", 3, (short) 1),
				new NewTopic("order.events", 3, (short) 1));
		try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
			admin.createTopics(topics).all().get();
		}
	}

	private static void createDatabases(String... names) throws Exception {
		try (Connection connection = DriverManager.getConnection(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
				Statement statement = connection.createStatement()) {
			for (String name : names) {
				statement.execute("CREATE DATABASE " + name);
			}
		}
	}

	private static void awaitUntil(BooleanSupplier condition, String description) {
		long deadline = System.nanoTime() + AWAIT_TIMEOUT.toNanos();
		while (System.nanoTime() < deadline) {
			if (condition.getAsBoolean()) {
				return;
			}
			sleep(POLL_INTERVAL);
		}
		throw new AssertionError("Timed out after " + AWAIT_TIMEOUT.toSeconds() + "s waiting for " + description);
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting", e);
		}
	}

	private static void close(ConfigurableApplicationContext context) {
		if (context != null) {
			context.close();
		}
	}
}
