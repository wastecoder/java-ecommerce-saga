# 🛒 ShopFlow

O **ShopFlow** mantém uma operação de e-commerce distribuída consistente sem transações globais (2PC): cada
pedido dispara uma **SAGA orquestrada** sobre **Apache Kafka** que coordena reserva de estoque, pagamento e
confirmação entre microsserviços autônomos (hexagonais, *database-per-service*), com mensageria **idempotente**
e **compensação** automática nos cenários de falha. Projeto backend, com clientes simulados via REST
(Postman/`curl`/testes).

[![CI](https://github.com/wastecoder/shopflow/actions/workflows/ci.yml/badge.svg)](https://github.com/wastecoder/shopflow/actions/workflows/ci.yml)
[![Publish images](https://github.com/wastecoder/shopflow/actions/workflows/publish-images.yml/badge.svg)](https://github.com/wastecoder/shopflow/actions/workflows/publish-images.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.7-6DB33F)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.1%20Oakwood-6DB33F)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-231F20)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

---

## 📑 Sumário

- [Visão geral](#-visão-geral)
- [Arquitetura](#-arquitetura)
- [Fluxo da SAGA](#-fluxo-da-saga)
- [Stack](#-stack)
- [Como rodar](#-como-rodar)
- [O fluxo na prática](#-o-fluxo-na-prática)
- [Observabilidade](#-observabilidade)
- [Segurança](#-segurança)
- [Testes e qualidade](#-testes-e-qualidade)
- [Decisões em destaque](#-decisões-em-destaque)
- [Documentação](#-documentação)
- [Roadmap](#-roadmap)

---

## 🎯 Visão geral

Um pedido (`POST /orders`) dispara uma **SAGA orquestrada** pelo `order-service`: reservar estoque →
autorizar pagamento → confirmar — com **compensação** automática (liberar estoque) quando o pagamento falha.
A comunicação entre serviços é **100% assíncrona** via Kafka (comandos e eventos), com consumidores
**idempotentes** e **Dead Letter Topics** por consumidor.

Objetivos de aprendizado exercitados aqui:

- **Mensageria & SAGA** — Spring Kafka (KRaft), produtor idempotente, ordenação por `orderId`, DLT.
- **Microsserviços** — API Gateway, service discovery (Eureka), arquitetura **hexagonal** e **database-per-service**.
- **Qualidade** — testes de integração com **Testcontainers**, gates de **JaCoCo + Pitest**, OpenAPI/Swagger.
- **Observabilidade** — métricas (Prometheus/Grafana) e tracing distribuído (Zipkin), inclusive **pelo Kafka**.
- **Segurança** — OAuth2/JWT com Keycloak (gateway + resource servers).
- **CI/CD** — GitHub Actions (build + testes) e publicação de imagens no GHCR.

A visão de longo prazo (pós-MVP) está em [`doc/FUTURE-PROGRESS.md`](doc/FUTURE-PROGRESS.md).

---

## 🏗 Arquitetura

Arquitetura hexagonal (ports & adapters) por serviço; dependências apontam para dentro
(`adapter → application → domain`). Cada serviço tem seu próprio schema no PostgreSQL
(*database-per-service*) e se comunica com os demais apenas por Kafka.

```mermaid
flowchart TB
    Client(["Cliente<br/>REST · Postman · curl"])

    subgraph edge["Borda"]
        GW["api-gateway :8080<br/>OAuth2 · roteamento lb://"]
    end

    subgraph core["Serviços de domínio · hexagonal · database-per-service"]
        ORD["order-service :8081<br/>orquestrador da SAGA"]
        INV["inventory-service :8082"]
        PAY["payment-service :8083<br/>PSP mock"]
        NOT["notification-service :8084"]
    end

    subgraph platform["Plataforma"]
        EUREKA["discovery-server :8761<br/>Eureka"]
        KAFKA[["Apache Kafka · KRaft :9092"]]
        PG[("PostgreSQL :5432<br/>um banco por serviço")]
        KC["Keycloak :8088<br/>realm shopflow"]
    end

    subgraph obs["Observabilidade"]
        PROM["Prometheus :9090"]
        GRAF["Grafana :3000"]
        ZIP["Zipkin :9411"]
    end

    Client -->|JWT Bearer| GW
    GW -->|lb:// via Eureka| ORD
    GW --> INV
    GW --> PAY
    GW --> NOT
    KC -.emite JWT.-> GW
    GW -.descobre.-> EUREKA

    ORD <-->|commands · events| KAFKA
    INV <--> KAFKA
    PAY <--> KAFKA
    KAFKA -->|order.events| NOT

    ORD --- PG
    INV --- PG
    PAY --- PG
    NOT --- PG

    core -.métricas.-> PROM
    PROM --> GRAF
    core -.spans.-> ZIP
```

Estrutura de pacotes, fluxo da SAGA (sequência + máquina de estados), contratos de evento e modelo de
domínio em [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md).

---

## 🔄 Fluxo da SAGA

O `order-service` é o **orquestrador**: emite comandos e reage às respostas, avançando ou **compensando** a
saga. Estados do pedido: `PENDING → STOCK_RESERVED → PAID → CONFIRMED` (e os terminais `REJECTED` / `CANCELLED`).
Desfechos: pagamento autorizado → `CONFIRMED`; recusado → `CANCELLED` com `ReleaseStock` (compensação); sem
estoque → `REJECTED`.

**Tópicos Kafka:** `inventory.commands`, `payment.commands`, `order.events`, `inventory.events`,
`payment.events` (+ um `<topic>.DLT` por consumidor). Envelope JSON `{ eventId, type, orderId, occurredAt, payload }`,
chave de partição = `orderId`. O **diagrama de sequência** completo e a máquina de estados estão em
[`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md#3-fluxo-da-saga).

---

## 🧰 Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / build | **Java 21**, **Gradle 9.5.1** (Kotlin DSL), monorepo multi-projeto |
| Framework | **Spring Boot 4.0.7**, **Spring Cloud 2025.1.1** ("Oakwood") |
| Mensageria | **Apache Kafka** (KRaft) + **Spring Kafka** (produtor idempotente, DLT) |
| Persistência | **PostgreSQL** (database-per-service), **Flyway**, **MapStruct 1.6.3** |
| Discovery / Gateway | **Eureka** + **Spring Cloud Gateway** (roteamento `lb://`) |
| Observabilidade | **Micrometer → Prometheus → Grafana**; **tracing → Zipkin** (Brave) |
| Segurança | **Keycloak** (OAuth2 / JWT), gateway + resource servers |
| Testes / qualidade | **Testcontainers**, **JUnit 5**, **JaCoCo**, **Pitest**, **springdoc/OpenAPI 3.0.3** |
| CI/CD | **GitHub Actions** (build + testes) + publicação de imagens no **GHCR** |

As decisões por trás dessas escolhas estão nos [ADRs](doc/README.md#adrs).

---

## 🚀 Como rodar

**Pré-requisitos:** Docker (+ Docker Compose) e JDK 21. No Windows, use `gradlew.bat` no lugar de `./gradlew`.

```bash
# 1) Sobe a infraestrutura (Kafka, PostgreSQL, Keycloak, Prometheus, Grafana, Zipkin, Kafka UI)
docker compose up -d

# 2) Sobe os serviços (em terminais separados; o discovery primeiro)
./gradlew :discovery-server:bootRun
./gradlew :api-gateway:bootRun
./gradlew :order-service:bootRun
./gradlew :inventory-service:bootRun     # popula 4 produtos de exemplo (profile "seed")
./gradlew :payment-service:bootRun
./gradlew :notification-service:bootRun
```

Os serviços rodam **no host**; o `docker-compose.yml` sobe apenas a infraestrutura. Comandos, URLs úteis,
profiles e configuração em [`doc/DEVELOPMENT.md`](doc/DEVELOPMENT.md).

---

## ▶️ O fluxo na prática

Caminho feliz (pedido → `CONFIRMED`), entrando pela borda (`api-gateway :8080`) com um JWT real do Keycloak:
obtenha um token de `customer`, faça `POST /orders` (total < 1000) e, em ~1s, consulte
`GET /orders/{id}` (→ `CONFIRMED`), `GET /payments/{id}` e `GET /notifications/{id}`.

O **walkthrough completo** com `curl` (token, payload, respostas esperadas, estoque semeado e os desfechos
de cancelamento/rejeição) está em [`doc/API.md`](doc/API.md#4-walkthrough-do-fluxo). Prefere uma **coleção
importável**? Há uma coleção **Postman** + script **HTTPie** prontos em [`postman/`](postman/README.md).

---

## 📈 Observabilidade

- **Métricas** — cada serviço expõe `/actuator/prometheus`; o **Prometheus** (`:9090`) raspa todos. Além das
  métricas padrão (HTTP, JVM, Kafka), há contadores de negócio (`shopflow_orders_placed_total`,
  `shopflow_saga_outcome_total`, …).
- **Dashboards** — o **Grafana** (`:3000`) provisiona **ShopFlow Overview** e **ShopFlow JVM**.
- **Tracing** — **Zipkin** (`:9411`) com Micrometer Tracing (Brave); o trace é propagado por HTTP **e pelo
  Kafka** (W3C `traceparent`), então uma saga inteira aparece como **um único trace**.

Guia operacional em [`doc/OBSERVABILITY.md`](doc/OBSERVABILITY.md).

---

## 🔐 Segurança

Autenticação via **Keycloak** (realm `shopflow`, roles `CUSTOMER` / `ADMIN`). O **gateway** exige um JWT válido
na borda e **encaminha** o `Authorization` para os serviços, que atuam como **resource servers** e aplicam as
regras por endpoint (defesa em profundidade):

| Endpoint | Exigência |
|---|---|
| `POST /orders` | role **CUSTOMER** |
| `GET /stock`, `GET /stock/{productId}` | role **ADMIN** |
| `GET /orders/{id}`, `GET /payments/{orderId}`, `GET /notifications/**` | autenticado |
| `/actuator/**`, Swagger | aberto |

Usuários de teste: `customer` / `customer` (CUSTOMER) e `manager` / `manager` (ADMIN). Detalhes (obter/inspecionar
JWT, exemplos `curl`) em [`doc/SECURITY.md`](doc/SECURITY.md).

---

## ✅ Testes e qualidade

```bash
./gradlew test              # testes unitários (sem Docker)
./gradlew integrationTest   # integração com Testcontainers (Kafka + PostgreSQL + Keycloak)
./gradlew check             # verificação completa: unit → integração → mutação + gates
```

Gates ligados ao `check`: **JaCoCo** (LINE ≥ 85% / BRANCH ≥ 75%) e **Pitest** (threshold 80%, mutators STRONGER).
Inclui um teste **ponta a ponta** que sobe os quatro serviços contra Kafka/PostgreSQL/Keycloak reais. Estratégia,
Object Mother e estilo de teste em [`doc/TESTS.md`](doc/TESTS.md).

---

## 🧭 Decisões em destaque

| ADR | Tema |
|---|---|
| [0001](doc/adr/0001-arquitetura-hexagonal-horizontal.md) | Arquitetura hexagonal (ports & adapters) horizontal |
| [0002](doc/adr/0002-saga-orquestrada.md) | SAGA orquestrada (orchestration), não coreografia |
| [0003](doc/adr/0003-kafka-spring-kafka-kraft.md) | Kafka via Spring Kafka direto, em modo KRaft |
| [0004](doc/adr/0004-database-per-service-postgresql.md) | Database-per-service com PostgreSQL |
| [0005](doc/adr/0005-idempotencia-e-dlt.md) | Consumidores idempotentes + Dead Letter Topic |
| [0006](doc/adr/0006-problem-details-rfc7807.md) | Tratamento de erros com Problem Details (RFC 7807) |

---

## 📚 Documentação

| Documento | Conteúdo |
|---|---|
| [`doc/README.md`](doc/README.md) | Índice da documentação e por onde começar |
| [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md) | Hexagonal, fluxo da SAGA, contratos Kafka, modelo de domínio |
| [`doc/API.md`](doc/API.md) | Endpoints, erros (RFC 7807) e o walkthrough do fluxo |
| [`postman/`](postman/README.md) | Coleção Postman + script HTTPie (importável) com instruções de uso |
| [`doc/DEVELOPMENT.md`](doc/DEVELOPMENT.md) | Como rodar, comandos, URLs, profiles, Docker e CI/CD |
| [`doc/TESTS.md`](doc/TESTS.md) | Estratégia de testes, Object Mother e gates de qualidade |
| [`doc/OBSERVABILITY.md`](doc/OBSERVABILITY.md) | Métricas, dashboards e tracing |
| [`doc/SECURITY.md`](doc/SECURITY.md) | Keycloak, OAuth2/JWT, regras e exemplos |
| [`doc/CHALLENGE.md`](doc/CHALLENGE.md) | O *brief* do desafio (requisitos e critérios de aceite) |
| [`doc/PROGRESS.md`](doc/PROGRESS.md) · [`doc/FUTURE-PROGRESS.md`](doc/FUTURE-PROGRESS.md) | Roadmap do MVP e visão pós-MVP |

---

## 🗺 Roadmap

MVP construído por fases — **Fases 0 a 8 concluídas** (fundação, REST+JPA, Kafka+SAGA, Payment+Notification,
qualidade, observabilidade, segurança, CI/CD e acabamento de portfólio): **MVP completo**. A visão pós-MVP
está em [`doc/FUTURE-PROGRESS.md`](doc/FUTURE-PROGRESS.md); acompanhe o roadmap em [`doc/PROGRESS.md`](doc/PROGRESS.md).
