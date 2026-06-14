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
- [x] ~~`discovery-server` (Eureka) e `api-gateway` com uma rota dummy~~
  - **`discovery-server`**: `@EnableEurekaServer` na app class (config standalone já existia — `:8761`, `register-with-eureka`/`fetch-registry: false`). Dashboard em `http://localhost:8761`.
  - **`api-gateway`**: rota dummy via **bean `RouteLocator` em Java** (`config/GatewayRoutesConfig`) — `/dummy/**` → `StripPrefix=1` → **`lb://api-gateway`** (encaminha ao próprio actuator). Adicionado **`spring-cloud-starter-loadbalancer`** (necessário p/ `lb://` no gateway 2025.x). Actuator `gateway` exposto + habilitado (`management.endpoint.gateway.access: unrestricted`, sintaxe Boot 4).
  - **Verificado:** `:discovery-server:test` + `:api-gateway:test` verdes (2 `contextLoads`, sem Docker). Smoke manual: `API-GATEWAY` registrado no Eureka; `GET /dummy/actuator/health` → **200 `{"status":"UP"}`** (gateway resolveu o serviço via Eureka — critério da fase); `GET /actuator/gateway/routes` lista a `dummy-route`.
  - **Fora deste item:** rotas reais p/ `order-service`/`inventory-service`, filtros (rate limit, JWT), circuit breaker → Fases 1/2/6.
- **Critério:** ~~`docker compose up` sobe a infra e o gateway resolve um serviço via Eureka.~~ ✅ **Fase 0 concluída.**

## 🧩 Fase 1 — Order & Inventory (REST + JPA, hexagonal horizontal)
- [x] ~~`order-service`: domínio (Order/OrderItem/OrderStatus), portas, use cases, Flyway, `POST/GET /orders`~~
  - **Hexagonal horizontal** espelhando o `partners-api`. **Domínio**: records `Order`/`OrderItem` (Bean Validation) + factory `Order.place(...)` que **congela** o `unitPrice` do request e calcula `totalAmount`; enum `OrderStatus` (6 valores do §7, só `PENDING` usado); `DomainException` + `OrderNotFoundException`.
  - **Aplicação**: portas `PlaceOrderUseCase`/`GetOrderUseCase` (in), `OrderRepository` (out), `PlaceOrderCommand`; impls `@Service`/`@Transactional`, validação via `Validator`.
  - **Web**: `OrderController` (`POST /orders` → 201+`Location`; `GET /orders/{id}` → 200), DTOs record com `toCommand()`/`from()`, `GlobalExceptionHandler` (`@RestControllerAdvice`) + `ProblemType` (**RFC 7807**).
  - **Persistência**: `OrderEntity`/`OrderItemEntity` (JPA+Lombok, `@OneToMany`), `OrderJpaDatabase`, `OrderRepositoryImpl` (`@Transactional` p/ inicializar a coleção lazy ao mapear), **`OrderEntityMapper` (MapStruct** + `@AfterMapping` p/ back-ref). Flyway `V1__create_order_tables.sql`.
  - **Config**: datasource → `orderdb` (database-per-service; `spring.docker.compose.enabled: false`, dev sobe a infra com `docker compose up -d postgres`); `ddl-auto: validate`.
  - **Decisão de preço:** `unitPrice` por **snapshot no request** no MVP; `catalog-service` como autoridade de preço fica para o FUTURE (Fase 11). O `StockItem` (§7) segue só com disponibilidade.
  - **Verificado:** `:order-service:build` **verde** — unit (domínio/use cases/`@WebMvcTest`/mapper/ProblemType) + `OrderRepositoryImplIntegrationTest` (Testcontainers, valida Flyway); **JaCoCo LINE 0.96 / BRANCH 0.96**. Smoke manual: `POST /orders` → 201 (`PENDING`, `totalAmount` somado), `GET` → 200, id aleatório → 404 ProblemDetail.
- [x] ~~`inventory-service`: domínio (StockItem/StockReservation), `GET /stock`, seed inicial~~
  - **Hexagonal horizontal** (mesma convenção do order: `adapter.web.handler` + `adapter.persistence.{entity,mapper,database}`). **Domínio**: record `StockItem(productId, available, reserved)` (`productId` = identidade) + `DomainException`/`StockItemNotFoundException`. **`StockReservation` adiado p/ a Fase 2** (ver item abaixo).
  - **Aplicação**: `ListStockUseCase`/`GetStockUseCase`/`SeedStockUseCase` (in), `StockRepository` (out), `SeedResult`; impls `@Service`/`@Transactional`.
  - **Web**: `StockController` (`GET /stock` → lista; `GET /stock/{productId}` → 200/404), `StockResponse`, `GlobalExceptionHandler` + `ProblemType` (RFC 7807, inclui `STOCK_ITEM_NOT_FOUND`).
  - **Persistência**: `entity/StockItemEntity`, `mapper/StockItemEntityMapper` (MapStruct), `database/StockItemJpaDatabase`, `StockRepositoryImpl`. Flyway `V1__create_stock_tables.sql` (só `stock_items`).
  - **Seed inicial** (estilo `partners-api`): `adapter/seed/StockSeederRunner` (`@Profile("seed")` + `CommandLineRunner`) lê `seed/sample-stock.json` (4 product UUIDs fixos `a1..a4`) → `SeedStockUseCase` **idempotente**. `spring.profiles.active: seed` no dev; `test` nos testes.
  - **Config**: datasource → `inventorydb`; `ddl-auto: validate`; `docker.compose.enabled: false`.
  - **Verificado:** `:inventory-service:build` **verde** — unit + `StockRepositoryImplIntegrationTest` + `StockSeederIntegrationTest` (`@ActiveProfiles("seed")`, Testcontainers); **JaCoCo LINE 0.97 / BRANCH 1.00**. Smoke: `GET /stock` → 200 com os 4 itens semeados; `GET /stock/{id}` → 200; id aleatório → 404 ProblemDetail.
- [x] ~~Problem Details (RFC 7807) + `GlobalExceptionHandler`~~
  - Implementado em `order-service` e `inventory-service` (`adapter.web.handler` + `ProblemType` por serviço). Replicar nos próximos serviços.
- **Critério:** criar pedido (`PENDING`) e consultar estoque via REST (sem Kafka ainda).

## 🔄 Fase 2 — Kafka + SAGA orquestrada (núcleo)
- [x] ~~Configurar tópicos, produtor idempotente e consumidores~~
  - **Fundação Kafka (infra) nos serviços do núcleo da Fase 2: `order-service` e `inventory-service`.** Cada serviço ganhou (duplicado, sem módulo compartilhado — mesma convenção do `ProblemType`): `adapter/config/KafkaTopicsConfig` (beans `NewTopic` via `TopicBuilder`), `adapter/messaging/EventEnvelope` (o envelope de fio), porta `application/port/out/EventPublisher` e o adapter `adapter/messaging/out/KafkaEventPublisher`.
  - **Tópicos — produtor é dono** (3 partições, RF 1 no broker KRaft single-node): `order-service` declara `inventory.commands`, `payment.commands`, `order.events`; `inventory-service` declara `inventory.events`. O `KafkaAdmin` auto-configurado do Boot cria os tópicos no startup. `payment.events` nasce com o `payment-service` (Fase 3).
  - **Produtor idempotente** (config em `application.yaml`, auto-config do Boot — sem factory à mão): `acks=all`, `enable.idempotence=true`, `max.in.flight<=5`, key=`StringSerializer`. **Chave = orderId** em todo publish → ordenação por pedido (mesma partição).
  - **Serde JSON desacoplado entre serviços:** `JacksonJsonSerializer`/`JacksonJsonDeserializer` (o `JsonSerializer`/`JsonDeserializer` está `@Deprecated(forRemoval)` no Spring Kafka 4) com **type headers desligados** (`spring.json.add.type.headers=false` no produtor; `spring.json.use.type.headers=false` + `value.default.type=<EventEnvelope local>` + `trusted.packages` no consumidor). Envelope `{ eventId, type, orderId, occurredAt, payload }` com `payload` como `Object` (o consumidor recebe um `Map`; itens 2/3 convertem para DTO conforme `type`). **Consumidor pronto** (factory default auto-configurada): os itens 2/3 só adicionam `@KafkaListener`, sem mais fiação.
  - **Verificado:** `:order-service:check` + `:inventory-service:check` **verdes** — unit (`KafkaEventPublisherTest`, Mockito) + integração Testcontainers (`KafkaEnvelopeRoundTripIntegrationTest`: publica via `EventPublisher` e consome com `@KafkaListener` só-de-teste, afirmando chave=orderId, round-trip completo do envelope/payload e **ausência do header `__TypeId__`**); **JaCoCo** (LINE/BRANCH) e **Pitest** (10/10 mutações, 100%) passam. Stack confirmada: Spring Boot 4.0.7, Spring Kafka 4.0.6, kafka-clients 4.1.2.
  - **Caveat (test config):** o `src/test/resources/application.yaml` **sombreia** o de `src/main` (o Boot carrega `classpath:/application.yaml` só da primeira raiz do classpath), então o bloco `spring.kafka` foi **espelhado no yaml de teste** — mesmo motivo pelo qual o `problem-details.base-uri` já era duplicado lá.
  - **Fora deste item:** `@KafkaListener` concretos + `OrderSagaCoordinator` (item 2); `StockReservation` + consumo de `inventory.commands` (item 3); `processed_messages` + `DeadLetterPublishingRecoverer`/tópicos `*.DLT` (item 4); infra Kafka de `payment-service`/`notification-service` (Fase 3).
- [x] ~~`OrderSagaCoordinator`: `ReserveStock → ProcessPayment → Confirm` e compensação `ReleaseStock`~~
  - **`OrderSagaCoordinator`** (`application/usecase`) implementa as in-ports `HandleStockReplyUseCase`/`HandlePaymentReplyUseCase`; orquestra carregando o pedido, aplicando uma **transição de domínio** e publicando o próximo passo. Dois `@KafkaListener` enxutos (`adapter/messaging/in`: `StockReplyListener` em `inventory.events`, `PaymentReplyListener` em `payment.events`) roteiam por `envelope.type()` usando o `orderId` do topo do envelope.
  - **Transições no agregado `Order`** (imutável, retornam novo `Order` ou lançam `InvalidOrderStateException`): `markStockReserved` (PENDING→STOCK_RESERVED), `markPaid` (STOCK_RESERVED→PAID), `confirm` (PAID→CONFIRMED), `reject` (PENDING→REJECTED), `cancel` (STOCK_RESERVED→CANCELLED). `PAID` é **transitório** dentro do handler de `PaymentAuthorized` (`markPaid().confirm()`), persistindo só `CONFIRMED`.
  - **Fluxo:** `PlaceOrder` persiste PENDING, emite **`OrderCreated`** (`order.events`) e o comando **`ReserveStock`** (`inventory.commands`). `StockReserved`→`ProcessPayment`. `PaymentAuthorized`→CONFIRMED + `OrderConfirmed`. `PaymentFailed`→**compensação** `ReleaseStock` + CANCELLED + `OrderCancelled`. `StockReservationFailed`→**REJECTED + `OrderRejected`** (corrigido o lapso do §6 do CHALLENGE, que dizia `OrderCancelled`). `StockReleased` é ack/no-op observável.
  - **Portas de saída de domínio** `OrderCommandPublisher`/`OrderEventPublisher` (conforme §9) sobre o `EventPublisher` genérico do item 1 (reaproveitado); a aplicação não conhece tópicos nem tipos. Nomes de tópicos e tipos centralizados em `adapter/messaging/Topics` e `MessageType`.
  - **Idempotência parcial via guardas de estado:** réplicas duplicadas/fora-de-ordem/estado terminal lançam `InvalidOrderStateException` → o coordinator registra e ignora (sem save, sem publish); `orderId` inexistente e tipo desconhecido também são ignorados (commitam o offset). Dedup real (`processed_messages`) + DLT ficam no **item 4**. Falha de publish propaga (redelivery). DB+Kafka não-atômicos (Outbox = FUTURE).
  - **Verificado:** `:order-service:check` **verde** + **Pitest 100%** (23/23 mutações; coordinator e `PlaceOrderUseCaseImpl` são alvos). Unit (coordinator 13, transições 10, listeners 4+3, place 2) + integração Testcontainers `OrderSagaIntegrationTest` (4 cenários) que **simula** estoque/pagamento (publicando respostas falsas via o próprio `EventPublisher`) e cobre confirma / sem-estoque-rejeita / pagamento-recusado-cancela+compensa / **redelivery idempotente**.
  - **Fora deste item:** consumidor de `inventory.commands` + domínio de reserva (**item 3**); `payment-service` (**Fase 3**) — ambos **simulados** nos testes; `processed_messages` + DLT (**item 4**).
- [x] ~~**inventory: `StockReservation`/`ReservationStatus` + tabela `stock_reservations` + casos de uso ReserveStock/ReleaseStock** (adiado da Fase 1; nasce junto do consumidor `inventory.commands`)~~
  - **Domínio**: record imutável `StockReservation(id, orderId, productId, quantity, status)` (§7) com factories `reserve(...)`→RESERVED e `failed(...)`→FAILED e transição `release()` (RESERVED→RELEASED; ilegal lança `IllegalReservationStateException`); enum `ReservationStatus {RESERVED, RELEASED, FAILED}`. O record `StockItem` ganhou comportamento `canReserve/reserve/release` (lança `InsufficientStockException`/`IllegalStockReleaseException`). Domínio sem Spring/JPA.
  - **Aplicação**: in-ports `ReserveStockUseCase`/`ReleaseStockUseCase` (`execute(StockCommand)`); out-ports `StockReservationRepository` (+`findByOrderId`/`findByOrderIdAndStatus`) e `InventoryEventPublisher` (porta de domínio espelhando o `OrderEventPublisher`); viewmodel `StockCommand(orderId, items)`. Impls `@Service`/`@Transactional` que **publicam a própria resposta** (listener fino, como o coordinator do order).
  - **ReserveStock all-or-nothing**: valida todos os itens antes de aplicar — sucesso decrementa o estoque + grava reservas RESERVED + `StockReserved`; falta/inexistente grava reservas **FAILED** (trilha de auditoria, **sem** mexer no estoque) + `StockReservationFailed`. **ReleaseStock** usa as reservas RESERVED persistidas como fonte da verdade (estoque volta ao valor original, reservas→RELEASED, `StockReleased`); ack idempotente se não houver reservas.
  - **Idempotência parcial via guardas de estado** (igual ao item 2; dedup real por `eventId`/`processed_messages` fica no item 4): redelivery com reservas RESERVED republica `StockReserved`, com FAILED republica `StockReservationFailed`, sem reprocessar. A aresta `ReserveStock` após um release completo fica para o item 4.
  - **Mensageria/persistência**: `adapter/messaging/in/StockCommandListener` (`@KafkaListener` em `inventory.commands`, converte o payload `Map`→`StockCommand` via `ObjectMapper`, roteia por `envelope.type()`); `InventoryEventPublisherImpl` sobre o `EventPublisher` genérico; `Topics`/`MessageType`/`StockReplyPayload`. Flyway `V2__create_stock_reservations.sql`; `StockReservationEntity` + `StockReservationEntityMapper` (MapStruct) + `JpaDatabase` + `RepositoryImpl`. `inventory.commands` **não** é declarado aqui (produtor é dono = order).
  - **Verificado:** `:inventory-service:check` **verde** — unit + integração Testcontainers (`StockCommandFlowIntegrationTest`: reserva-feliz / insuficiente / release ponta-a-ponta, afirmando resposta **e** efeitos no banco; `StockReservationRepositoryImplIntegrationTest`); **Pitest 97%** (33/34, test strength **100%**; os use cases são alvos). JaCoCo passa.
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
