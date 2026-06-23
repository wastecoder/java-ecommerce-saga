# Desenvolvimento

Tudo o que você precisa para rodar e mexer no ShopFlow localmente. No Windows use `gradlew.bat` no
lugar de `./gradlew`.

- [1. Pré-requisitos](#1-pré-requisitos)
- [2. Subir o ambiente](#2-subir-o-ambiente)
- [3. Comandos úteis](#3-comandos-úteis)
- [4. URLs úteis](#4-urls-úteis)
- [5. Profiles e seed](#5-profiles-e-seed)
- [6. Configuração](#6-configuração)
- [7. Docker](#7-docker)
- [8. CI/CD](#8-cicd)
- [9. Onde achar as coisas no código](#9-onde-achar-as-coisas-no-código)

## 1. Pré-requisitos

- **JDK 21** (o build usa toolchain Gradle; basta ter um JDK 21 disponível).
- **Docker** (+ Docker Compose) em execução — para a infraestrutura local e para os testes de
  integração (Testcontainers).
- O wrapper do Gradle já vem versionado (`gradlew.bat` / `gradlew`, Gradle **9.5.1**). Não precisa
  instalar Gradle.

## 2. Subir o ambiente

O `docker-compose.yml` sobe **apenas a infraestrutura**; os serviços Spring rodam **no host** via
`bootRun` (`spring.docker.compose.enabled: false` — o `bootRun` não sobe containers sozinho).

### 2.1 Infraestrutura

```bash
docker compose up -d        # Kafka, PostgreSQL, Keycloak, Prometheus, Grafana, Zipkin, Kafka UI
docker compose ps           # status
docker compose down         # para tudo (mantém volumes)
docker compose down -v      # para e zera os volumes (banco limpo)
```

### 2.2 Serviços (um terminal por serviço; o discovery primeiro)

```bash
./gradlew :discovery-server:bootRun      # Eureka       :8761  (suba primeiro)
./gradlew :api-gateway:bootRun           # gateway      :8080
./gradlew :order-service:bootRun         # pedidos      :8081
./gradlew :inventory-service:bootRun     # estoque      :8082  (profile "seed": 4 produtos de exemplo)
./gradlew :payment-service:bootRun       # pagamento    :8083
./gradlew :notification-service:bootRun  # notificação  :8084
```

Suba o `discovery-server` antes para os demais se registrarem no Eureka.

## 3. Comandos úteis

| Comando | O que faz |
|---|---|
| `./gradlew :<service>:bootRun` | Roda um serviço (no host). |
| `./gradlew build` | Build completo: compila + unit + integração (com Docker) + gates. |
| `./gradlew test` | Testes unitários (exclui `*IntegrationTest`; **não** exige Docker). |
| `./gradlew integrationTest` | Testes de integração (Testcontainers; exige Docker). |
| `./gradlew check` | Verificação completa: unit → integração → cobertura → mutação. |
| `./gradlew pitest` | Mutation testing (serviços de domínio). |
| `./gradlew jacocoTestReport` | Relatório de cobertura (HTML em `build/reports/jacoco`). |
| `./gradlew test --tests "com.wastecoder.shopflow.order.*SomeTest"` | Uma classe de teste. |

Estratégia de testes, gates e relatórios em [TESTS.md](TESTS.md).

## 4. URLs úteis

| Recurso | URL |
|---|---|
| API Gateway (entrada única) | http://localhost:8080 |
| Eureka (dashboard) | http://localhost:8761 |
| Swagger — order / inventory / payment / notification | `:8081/swagger-ui.html` · `:8082` · `:8083` · `:8084` |
| Kafka UI | http://localhost:8085 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Zipkin | http://localhost:9411 |
| Keycloak (admin) | http://localhost:8088 |

## 5. Profiles e seed

- **`seed`** — o `inventory-service` ativa o profile `seed` em dev. O `StockSeederRunner`
  (`@Profile("seed")`, em `adapter/seed`) carrega `seed/sample-stock.json` no startup: 4 produtos de
  exemplo (`a1111111…`=100, `a2222222…`=50, `a3333333…`=10, `a4444444…`=0). Ver a tabela em
  [API.md](API.md#4-walkthrough-do-fluxo).
- **PSP mock** — o `payment-service` recusa pagamentos com `amount ≥ decline-threshold`
  (`shopflow.payment.psp.mode: DECLINE_ABOVE_THRESHOLD`, `decline-threshold: 1000.00`).

## 6. Configuração

Cada serviço tem seu `src/main/resources/application.yaml`. Credenciais e portas da infraestrutura são
parametrizadas por variáveis no [`.env.example`](../.env.example) versionado — copie para um `.env`
local (gitignored) e ajuste; sem `.env`, valem os defaults, então `docker compose up` funciona num
clone novo.

Chaves notáveis dos serviços de domínio:

| Chave | Valor (dev) | Para quê |
|---|---|---|
| `spring.application.name` | `<service>` | Nome no Eureka. |
| `server.port` | `8081`–`8084` | Porta do serviço. |
| `eureka.client.service-url.defaultZone` | `http://localhost:8761/eureka` | Registro no discovery. |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Broker Kafka. |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/<db>` | Banco do serviço (database-per-service). |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Schema é versionado por **Flyway**. |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `${KEYCLOAK_ISSUER_URI:http://localhost:8088/realms/shopflow}` | Validação do JWT. |
| `management.tracing` / `...zipkin` | `http://localhost:9411/api/v2/spans` (sampling 100%) | Tracing distribuído. |
| `spring.docker.compose.enabled` | `false` | `bootRun` **não** sobe containers; infra via `docker compose`. |

Variáveis do `.env.example`: `POSTGRES_USER/PASSWORD/DB/PORT`, `KAFKA_PORT`, `KAFKA_UI_PORT`,
`PROMETHEUS_PORT`, `GRAFANA_PORT`, `GRAFANA_ADMIN_USER/PASSWORD`, `ZIPKIN_PORT`, `KEYCLOAK_PORT`,
`KEYCLOAK_ADMIN/PASSWORD`.

## 7. Docker

Serviços do [`docker-compose.yml`](../docker-compose.yml) (infraestrutura):

| Serviço | Imagem | Porta (host) | Notas |
|---|---|---|---|
| `postgres` | `postgres:17` | 5432 | `infra/init-db.sql` cria `orderdb`/`inventorydb`/`paymentdb`/`notificationdb`. |
| `kafka` | `apache/kafka-native:4.3.0` | 9092 | KRaft (sem Zookeeper); listener interno `29092`. |
| `kafka-ui` | `kafbat/kafka-ui` | 8085 | UI web do Kafka. |
| `prometheus` | `prom/prometheus:v3.1.0` | 9090 | Config `infra/prometheus.yml`. |
| `grafana` | `grafana/grafana:11.4.0` | 3000 | Provisionado em `infra/grafana/provisioning`. |
| `zipkin` | `openzipkin/zipkin:3.6.0` | 9411 | Armazenamento em memória (dev). |
| `keycloak` | `quay.io/keycloak/keycloak:26.6.3` | 8088 | Dev mode, `--import-realm` de `infra/keycloak/realm-export.json`. |

**Imagens dos serviços** — há um `Dockerfile` por serviço (imagem fina): base `eclipse-temurin:21-jre`,
usuário não-root `spring`, `COPY build/libs/app.jar` + `ENTRYPOINT java -jar`, `EXPOSE` da porta do
serviço. O jar é buildado pelo Gradle (`:<service>:bootJar`, nome previsível `app.jar`) antes do
`docker build`.

## 8. CI/CD

| Workflow | Gatilho | O que faz |
|---|---|---|
| [`ci.yml`](../.github/workflows/ci.yml) | PR e push para `main`, manual | `./gradlew build --no-daemon --stacktrace` — gate completo (compila → unit → integração Testcontainers → JaCoCo → Pitest). |
| [`publish-images.yml`](../.github/workflows/publish-images.yml) | push para `main`, manual | Matriz dos 6 serviços: `:<service>:bootJar` → build da imagem → push no **GHCR** (`ghcr.io/<owner>/shopflow-<service>`), tags `latest` + short SHA. |

## 9. Onde achar as coisas no código

| O que você quer mexer | Onde está |
|---|---|
| Endpoints HTTP | `adapter/web/*Controller` |
| Mapeamento de erros → HTTP | `adapter/web/handler/` (`GlobalExceptionHandler`, `ProblemType`) |
| Regra de negócio / saga | `application/usecase/` (`OrderSagaCoordinator` no order-service) |
| Consumidores Kafka | `adapter/messaging/in/*Listener` |
| Produtores Kafka | `adapter/messaging/out/` |
| Nomes de tópicos / tipos | `adapter/messaging/Topics`, `adapter/messaging/MessageType` |
| Idempotência / DLT | `adapter/messaging/in/IdempotentMessageProcessor`, `adapter/config/KafkaErrorHandlingConfig` |
| Segurança | `adapter/config/SecurityConfig` (+ `KeycloakRealmRoleConverter`) |
| Persistência | `adapter/persistence/` |
| Seed de estoque | `inventory-service/adapter/seed/` |
| PSP mock | `payment-service/adapter/psp/` |
| Configuração | `src/main/resources/application.yaml` |
| Infraestrutura local | `docker-compose.yml`, `infra/` |
