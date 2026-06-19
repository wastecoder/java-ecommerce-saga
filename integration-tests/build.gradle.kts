plugins {
	java
	id("io.spring.dependency-management")
}

// Test-only aggregator module: it has no `src/main` and produces no application. It boots the four real
// services (as separate Spring contexts) against shared Testcontainers to exercise the end-to-end saga.
// No Spring Boot plugin here on purpose, so there is no bootJar without a main class.

dependencyManagement {
	imports {
		// Same Spring Boot line as the root `org.springframework.boot` plugin; pulls the managed versions
		// (Spring Boot, Spring Kafka, Testcontainers, JUnit, AssertJ, ...) for the test dependencies below.
		mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.7")
		// Needed so the services' transitive Spring Cloud deps (e.g. eureka-client) resolve their versions
		// in this consumer module too.
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

dependencies {
	// The four services under test — exposes their main classes (application, controllers, ports, domain).
	testImplementation(project(":order-service"))
	testImplementation(project(":inventory-service"))
	testImplementation(project(":payment-service"))
	testImplementation(project(":notification-service"))

	// Same testing stack the services use (spring-kafka, data-jpa and web come transitively from the projects).
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-kafka")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	// Real Keycloak for the end-to-end test: imports the realm and issues a JWT so the order-service
	// resource server validates a genuine token (supports Keycloak 26.x via withRealmImportFile).
	testImplementation("com.github.dasniko:testcontainers-keycloak:3.8.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	// Needed by the test to CREATE the per-service databases in the shared Postgres container, and at
	// runtime by each booted context's datasource.
	testRuntimeOnly("org.postgresql:postgresql")
}

// No production code lives here, so the inherited JaCoCo coverage gate (LINE 85% / BRANCH 75%) has nothing
// to measure — disable it so `check` is not blocked by an empty bundle.
tasks.named("jacocoTestCoverageVerification") {
	enabled = false
}

// Put the single source-of-truth realm export (used by docker-compose) on the test classpath as
// keycloak/realm-export.json, so the end-to-end test's KeycloakContainer can import it.
tasks.processTestResources {
	from(rootProject.file("infra/keycloak")) {
		into("keycloak")
	}
}
