# 📦 Progresso — ShopFlow (MVP + Notification)

Roadmap em fases para construir o MVP. A sequência **fecha a saga cedo** e vai agregando qualidade.

> **Como usar:** ao concluir um item, marque a caixa **e** risque o texto:
> `- [x] ~~item concluído~~`. Mantenha este arquivo sempre em dia. A visão "Completo" (pós-MVP)
> está em `FUTURE-PROGRESS.md`.

## 🧱 Fase 0 — Fundação do monorepo
- [x] ~~Criar monorepo Gradle (Kotlin DSL): `settings.gradle.kts` + `build.gradle.kts` raiz (Java 21, BOMs, jacoco, pitest)~~
  - **6 projetos gerados no Spring Initializr** (Gradle-Kotlin, JAR, YAML, **Java 21**, **Boot 4.0.7**, **Spring Cloud 2025.1.1 "Oakwood"**, group `com.wastecoder.shopflow`) montados num **multi-projeto Gradle**: `settings.gradle.kts` inclui os 6 subprojetos; wrapper **Gradle 9.5.1** promovido à raiz; docs em `doc/`; estrutura achatada (sem sobras de zip — `HELP.md`/`compose.yaml`/settings duplicados removidos).
  - **`build.gradle.kts` raiz** centraliza tudo via `subprojects {}`: group/version, **toolchain Java 21**, `mavenCentral()`, BOM do Spring Cloud por serviço e as versões num só lugar (Boot 4.0.7, dependency-management 1.1.7, **MapStruct 1.6.3** + `lombok-mapstruct-binding` 0.2.0, **springdoc 3.0.3**, plugin Pitest 1.19.0 / core 1.25.4 / junit5-plugin 1.2.3). Cada subprojeto usa `plugins {}` **sem versão**.
  - **Convenção de testes** (espelha o `partners-api`): a task `test` roda só unitários (exclui `*IntegrationTest`, **sem Docker**) e uma task `integrationTest` separada roda os de integração com **Testcontainers** (Kafka + PostgreSQL); `failOnNoDiscoveredTests=false` (Gradle 9). Gates ligados ao `check`: **JaCoCo** BUNDLE (LINE ≥ 0.85 / BRANCH ≥ 0.75) e **Pitest** (mutators STRONGER, thresholds 80).
  - **Dependências fora do Initializr** adicionadas à mão: MapStruct (+`mapstruct-processor` +`lombok-mapstruct-binding`, ordem dos annotation processors lombok → binding → mapstruct), **springdoc-openapi** v3 (linha do Boot 4) nos 4 serviços de domínio, e os plugins **JaCoCo + Pitest** na raiz. (`spring-kafka-test` já veio do Initializr.)
  - **`application.yaml` por serviço**: `spring.application.name` + porta do §5 + `eureka...defaultZone` apontando p/ `:8761`. `discovery-server` é Eureka **standalone** (`auto-registration.enabled: false` + `register-with-eureka`/`fetch-registry: false`). Em `src/test` cada serviço recebe `eureka.client.enabled: false` (silencia conexão recusada).
  - **Testes gerados renomeados** para `*IntegrationTest` (`@Import(TestcontainersConfiguration)` + `@SpringBootTest`) com `@DisplayName` Given/When/Then — assim `gradlew test` roda sem Docker e o `contextLoads` com containers cai na `integrationTest`.
  - **Verificado:** `gradlew build` **verde** — 2 testes unitários (`api-gateway`, `discovery-server`) + 4 `contextLoads` subindo **Kafka + PostgreSQL reais** via Testcontainers (0 falhas); gates JaCoCo passam (módulos ainda sem código de produção); smoke do `pitest` resolve as versões. **Sem `git init`/commits** nesta etapa.
  - **Caveats / fora deste item:** `gradlew build` e `check` **exigem Docker** (o `check` encadeia `integrationTest` — é o critério da Fase 4). **Pitest × JUnit 6**: o `pitest-junit5-plugin` 1.2.3 ainda não suporta o JUnit 6 do Boot 4 oficialmente — confirmar "Ran N tests > 0" quando houver testes de verdade (Fase 4). Os próximos itens da Fase 0 (`docker-compose.yml` + `infra/init-db.sql`, `@EnableEurekaServer`, rota dummy do gateway) são **código**.
- [x] ~~`docker-compose.yml` com Kafka (KRaft) + Kafka UI + PostgreSQL + `infra/init-db.sql`~~
  - **`docker-compose.yml` na raiz** (`name: shopflow`, rede `shopflow`) com 3 serviços: **`postgres`** (`postgres:17`, healthcheck `pg_isready`, volume `postgres-data`), **`kafka`** (`apache/kafka-native:4.3.0`, **KRaft single-node** broker+controller, storage efêmero — a imagem roda como non-root e não escreve em volume nomeado root-owned; dev não precisa persistir o log) e **`kafka-ui`** (`kafbat/kafka-ui`, sucessor mantido do provectus) em **`:8085`** apontando p/ o cluster `shopflow`.
  - **Kafka dual-listener**: `INTERNAL://kafka:29092` (containers, ex.: kafka-ui) e `EXTERNAL://localhost:9092` (host / `bootRun`), + `CONTROLLER` interno. Replication factor 1 (dev). Sem healthcheck no kafka porque a imagem `*-native` é mínima (sem `bin/*.sh`); o kafka-ui já tem retry próprio (`depends_on: service_started`).
  - **`infra/init-db.sql`** cria os 4 bancos **database-per-service** (`orderdb`/`inventorydb`/`paymentdb`/`notificationdb`) — roda só na **primeira** init do volume (`/docker-entrypoint-initdb.d`).
  - **`.env.example`** (versionado) + **`.env`** (local, agora no `.gitignore` com `!.env.example`) com credenciais/portas; o compose usa `${VAR:-default}`, então sobe sem `.env`.
  - **Fora deste item:** os **tópicos** (`inventory.commands`, `payment.commands`, `order.events`, `inventory.events`, `payment.events` + `*.DLT`) virão de `KafkaTopicsConfig` (`NewTopic`) na **Fase 2**. O wiring de **datasource por serviço** + a interação do `spring-boot-docker-compose` no `bootRun` são da **Fase 1**.
- [ ] `discovery-server` (Eureka) e `api-gateway` com uma rota dummy
- **Critério:** `docker compose up` sobe a infra e o gateway resolve um serviço via Eureka.

## 🧩 Fase 1 — Order & Inventory (REST + JPA, hexagonal horizontal)
- [ ] `order-service`: domínio (Order/OrderItem/OrderStatus), portas, use cases, Flyway, `POST/GET /orders`
- [ ] `inventory-service`: domínio (StockItem/StockReservation), `GET /stock`, seed inicial
- [ ] Problem Details (RFC 7807) + `GlobalExceptionHandler`
- **Critério:** criar pedido (`PENDING`) e consultar estoque via REST (sem Kafka ainda).

## 🔄 Fase 2 — Kafka + SAGA orquestrada (núcleo)
- [ ] Configurar tópicos, produtor idempotente e consumidores
- [ ] `OrderSagaCoordinator`: `ReserveStock → ProcessPayment → Confirm` e compensação `ReleaseStock`
- [ ] Idempotência (`processed_messages`) + DLT por consumidor
- **Critério:** pedido feliz **confirma**; sem estoque **rejeita**; pagamento recusado **cancela com compensação**.

## 💳 Fase 3 — Payment & Notification
- [ ] `payment-service`: PSP mock com cenários de aprovação/recusa controláveis
- [ ] `notification-service`: consome `order.events` e registra notificação (mock/log)
- **Critério:** notificação registrada em confirm e cancel.

## ✅ Fase 4 — Qualidade
- [ ] Testes com **Testcontainers** (Kafka + PostgreSQL) cobrindo happy path + compensação
- [ ] Padrão **Object Mother** + `@DisplayName` Given/When/Then
- [ ] Gates **JaCoCo + Pitest** e **OpenAPI/Swagger** em todos os serviços
- **Critério:** `./gradlew check` verde com testes de integração.

## 📈 Fase 5 — Observabilidade
- [ ] Actuator + Prometheus + Grafana (dashboards)
- [ ] Tracing Micrometer → Zipkin com **propagação do trace pelo Kafka**
- **Critério:** trace de uma saga ponta-a-ponta no Zipkin; dashboard no Grafana.

## 🔐 Fase 6 — Segurança
- [ ] Keycloak (realm `shopflow`, roles `CUSTOMER`/`ADMIN`)
- [ ] Gateway OAuth2 + serviços como resource servers (JWT)
- **Critério:** `POST /orders` exige JWT válido.

## 🚀 Fase 7 — CI/CD
- [ ] GitHub Actions: build + `test` + `integrationTest` (Testcontainers)
- [ ] Build e push das imagens para o **GHCR** no merge para `main`
- **Critério:** pipeline verde no PR; imagens publicadas no merge.

## 🎁 Fase 8 — Acabamento de portfólio
- [ ] README com diagramas (mermaid), badges e GIF/asciinema do fluxo
- [ ] Coleção Postman/HTTPie + instruções de uso
- **Critério:** repositório "vendável" e fácil de rodar por terceiros.
