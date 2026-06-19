plugins {
	id("org.springframework.boot") version "4.0.7" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("info.solidsoft.pitest") version "1.19.0" apply false
}

extra["springCloudVersion"] = "2025.1.1"
extra["mapstructVersion"] = "1.6.3"
extra["lombokMapstructBindingVersion"] = "0.2.0"
extra["springdocVersion"] = "3.0.3"

val jacocoExcludes = listOf(
	"**/*Application.class",
	"**/adapter/web/dto/**",
)

subprojects {
	apply(plugin = "java")
	apply(plugin = "jacoco")

	group = "com.wastecoder.shopflow"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
	}

	configure<JavaPluginExtension> {
		toolchain {
			languageVersion = JavaLanguageVersion.of(21)
		}
	}

	tasks.withType<Test>().configureEach {
		useJUnitPlatform()
	}

	tasks.named<Test>("test") {
		exclude("**/*IntegrationTest.class", "**/*IntegrationTest\$*.class")
		// Modules whose only tests are integration tests have nothing left to run here
		failOnNoDiscoveredTests.set(false)
		finalizedBy(tasks.named("jacocoTestReport"))
	}

	val sourceSets = the<SourceSetContainer>()

	val integrationTest = tasks.register<Test>("integrationTest") {
		description = "Runs integration tests (Testcontainers Kafka/PostgreSQL)."
		group = "verification"
		testClassesDirs = sourceSets["test"].output.classesDirs
		classpath = sourceSets["test"].runtimeClasspath
		include("**/*IntegrationTest.class", "**/*IntegrationTest\$*.class")
		failOnNoDiscoveredTests.set(false)
		shouldRunAfter(tasks.named("test"))
		finalizedBy(tasks.named("jacocoTestReport"))
	}

	tasks.named<JacocoReport>("jacocoTestReport") {
		reports {
			xml.required.set(true)
			html.required.set(true)
		}
		executionData.setFrom(
			fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") }
		)
		classDirectories.setFrom(
			files(classDirectories.files.map {
				fileTree(it) { exclude(jacocoExcludes) }
			})
		)
	}

	tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
		dependsOn(tasks.named("test"), integrationTest)
		executionData.setFrom(
			fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") }
		)
		classDirectories.setFrom(
			files(classDirectories.files.map {
				fileTree(it) { exclude(jacocoExcludes) }
			})
		)
		violationRules {
			rule {
				element = "BUNDLE"
				limit {
					counter = "LINE"
					minimum = "0.85".toBigDecimal()
				}
				limit {
					counter = "BRANCH"
					minimum = "0.75".toBigDecimal()
				}
			}
		}
	}

	tasks.named("check") {
		dependsOn(integrationTest, tasks.named("jacocoTestCoverageVerification"))
	}

	// The deployable services produce an executable bootJar with a predictable name so each service's
	// Dockerfile can `COPY build/libs/app.jar` unambiguously (the Spring Boot plugin also emits a
	// `-plain.jar`). The plain `jar` stays enabled on purpose: the integration-tests module consumes the
	// services via project(":…"), which resolves their plain artifact.
	plugins.withId("org.springframework.boot") {
		tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
			archiveFileName.set("app.jar")
		}
	}

	// Pitest is applied only by the 4 domain services (they declare the plugin + their own targetClasses).
	// api-gateway and discovery-server have no application-layer logic / mutation targets — their config and
	// RouteLocator beans are covered by JaCoCo via each module's contextLoads test, so they stay off pitest.
	plugins.withId("info.solidsoft.pitest") {
		configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
			pitestVersion.set("1.25.4")
			junit5PluginVersion.set("1.2.3")
			// Testcontainers-based tests cannot run inside PIT's forked JVMs
			excludedTestClasses.set(setOf("*IntegrationTest", "*IntegrationTest\$*"))
			mutators.set(setOf("STRONGER"))
			timestampedReports.set(false)
			outputFormats.set(setOf("HTML", "XML"))
			mutationThreshold.set(80)
			coverageThreshold.set(80)
			failWhenNoMutations.set(false)
		}

		// Order the mutation run after the integration tests so a single `check` flows
		// unit -> integration -> mutation. This is ordering only (not a dependency).
		tasks.named("pitest") {
			mustRunAfter(integrationTest)
		}
		// Make the mutation gate part of `check` (CHALLENGE §12: check enforces JaCoCo AND Pitest).
		tasks.named("check") {
			dependsOn(tasks.named("pitest"))
		}
	}
}
