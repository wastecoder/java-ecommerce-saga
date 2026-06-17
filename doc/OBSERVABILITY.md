# 📈 Observabilidade — ShopFlow (Prometheus + Grafana)

Guia prático de como **operar** a stack de observabilidade do ShopFlow: subir a infra, rodar os
serviços, gerar dados e navegar pelo Prometheus e pelo Grafana até ver os painéis populados.

A coleta usa **Spring Boot Actuator + Micrometer**: cada serviço expõe `/actuator/prometheus`, o
**Prometheus** (container) raspa esses endpoints a cada 10s e o **Grafana** (container) lê do Prometheus
e desenha os dashboards.

> **Modelo de execução:** a infra (PostgreSQL, Kafka, Prometheus, Grafana) roda em **containers**, mas
> os 6 serviços Spring rodam no **host** via `bootRun`. Por isso o Prometheus os raspa por
> `host.docker.internal:<porta>` (resolve no Docker Desktop/Windows; em Linux o `extra_hosts:
> host-gateway` do compose garante o acesso ao host).
>
> **Pré-requisitos:** Docker Desktop em execução e JDK 21.

---

## 🐳 1. Subir a infra (Docker)

Na raiz do repositório:

```bash
docker compose up -d
```

Isso sobe: `postgres`, `kafka`, `kafka-ui`, **`prometheus` (:9090)** e **`grafana` (:3000)**.

As portas e credenciais saem do `.env` (com defaults caso a variável não exista):

| Variável | Default | Para quê |
|---|---|---|
| `PROMETHEUS_PORT` | `9090` | Porta do Prometheus no host |
| `GRAFANA_PORT` | `3000` | Porta do Grafana no host |
| `GRAFANA_ADMIN_USER` | `admin` | Usuário admin do Grafana |
| `GRAFANA_ADMIN_PASSWORD` | `admin` | Senha admin do Grafana |

Conferir que tudo subiu: `docker compose ps`.
Parar a infra: `docker compose down` (acrescente `-v` para **apagar os volumes** — cuidado, zera dados
do Postgres e do Grafana).

---

## 🚀 2. Rodar os microsserviços

Os serviços rodam no host. Cada `bootRun` **bloqueia o terminal**, então use **um terminal por
serviço**:

```bash
./gradlew.bat :discovery-server:bootRun
./gradlew.bat :order-service:bootRun
./gradlew.bat :inventory-service:bootRun
./gradlew.bat :payment-service:bootRun
./gradlew.bat :notification-service:bootRun
./gradlew.bat :api-gateway:bootRun
```

| Serviço | Porta |
|---|---|
| api-gateway | 8080 |
| order-service | 8081 |
| inventory-service | 8082 |
| payment-service | 8083 |
| notification-service | 8084 |
| discovery-server | 8761 |

**Mínimo para exercitar a saga e gerar as métricas de negócio:** `order` + `inventory` + `payment`
(esses três já produzem todos os contadores `shopflow_*`). Suba também o `notification-service` para o
fluxo completo — ele apenas consome os eventos terminais para "enviar" notificações, não altera o
desfecho da saga nem os contadores.

Notas:
- Suba o **`discovery-server` primeiro** — sem ele os demais logam "connection refused" no Eureka (mas
  funcionam mesmo assim, só fica barulhento). O `api-gateway` é opcional para gerar pedidos.
- O `inventory-service` **semeia o estoque automaticamente** (`spring.profiles.active: seed` no
  `application.yaml`) — não precisa de flag extra.
- No Prometheus, cada alvo aparece **DOWN** enquanto o serviço correspondente não estiver de pé.

---

## 📨 3. Gerar dados (Swagger)

Abra o Swagger UI do order-service: **`http://localhost:8081/swagger-ui.html`** → endpoint
**`POST /orders`** → **Try it out** → cole um dos corpos abaixo → **Execute**.

> `customerId` pode ser qualquer UUID. O total do pedido é `Σ (quantity × unitPrice)`.

Produtos já semeados no inventory (`seed/sample-stock.json`):

| productId | disponível |
|---|---|
| `a1111111-1111-1111-1111-111111111111` | 100 |
| `a2222222-2222-2222-2222-222222222222` | 50 |
| `a3333333-3333-3333-3333-333333333333` | 10 |
| `a4444444-4444-4444-4444-444444444444` | **0** |

### ✅ Aceito → `CONFIRMED`
Produto com estoque e total **`< 1000.00`** (o PSP aprova):

```json
{
  "customerId": "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "items": [
    { "productId": "a1111111-1111-1111-1111-111111111111", "quantity": 2, "unitPrice": 400.00 }
  ]
}
```

Total 800.00 → reserva ok → pagamento autorizado → pedido **CONFIRMED**.
Liga: `shopflow_orders_placed_total`, `shopflow_reservations_outcome_total{outcome="reserved"}`,
`shopflow_payments_outcome_total{outcome="authorized"}`, `shopflow_saga_outcome_total{outcome="confirmed"}`.

### ❌ Rejeitado → `REJECTED`
Produto **sem estoque** (`a4444444…`, 0 disponível):

```json
{
  "customerId": "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "items": [
    { "productId": "a4444444-4444-4444-4444-444444444444", "quantity": 1, "unitPrice": 50.00 }
  ]
}
```

A reserva falha → pedido **REJECTED** (o pagamento nem chega a rodar).
Liga: `shopflow_reservations_outcome_total{outcome="failed"}`,
`shopflow_saga_outcome_total{outcome="rejected"}`.

### ↩️ Cancelado → `CANCELLED`
Produto com estoque, mas total **`≥ 1000.00`** (o PSP recusa). Regra do mock PSP:
`DECLINE_ABOVE_THRESHOLD` com limite **1000.00**.

```json
{
  "customerId": "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "items": [
    { "productId": "a1111111-1111-1111-1111-111111111111", "quantity": 2, "unitPrice": 600.00 }
  ]
}
```

Total 1200.00 → reserva ok, PSP recusa, estoque é liberado (compensação) → pedido **CANCELLED**.
Liga: `shopflow_payments_outcome_total{outcome="failed"}`,
`shopflow_reservations_outcome_total{outcome="released"}`,
`shopflow_saga_outcome_total{outcome="cancelled"}`.

---

## 🔎 4. Como usar o Prometheus (`http://localhost:9090`)

O Prometheus é o **banco de séries temporais + motor de consultas (PromQL)** — é a fonte de dados do
Grafana, mas também dá para consultá-lo direto.

- **Status → Targets:** mostra os 6 jobs (um por serviço) com o estado **UP/DOWN**. Útil para confirmar
  que o Prometheus está conseguindo raspar cada serviço.
- **Endpoint cru de um serviço** (o que o Prometheus lê): `GET http://localhost:8081/actuator/prometheus`
  (e a saúde em `http://localhost:8081/actuator/health`).
- **Aba de consulta:** cole uma expressão PromQL e clique em *Execute* (veja em *Table* ou *Graph*). Como
  o scrape é a cada 10s, as séries aparecem em poucos segundos após disparar um pedido.

Consultas de exemplo (as mesmas que alimentam os dashboards):

```promql
up
sum(shopflow_orders_placed_total)
sum by (outcome) (shopflow_saga_outcome_total)
sum by (outcome) (rate(shopflow_payments_outcome_total[5m]))
sum by (outcome) (rate(shopflow_reservations_outcome_total[5m]))
sum by (application) (rate(http_server_requests_seconds_count[1m]))
histogram_quantile(0.95, sum by (le, application) (rate(http_server_requests_seconds_bucket[5m])))
sum by (application) (jvm_memory_used_bytes{area="heap"})
sum by (application) (kafka_consumer_fetch_manager_records_lag)
```

---

## 📊 5. Como usar o Grafana (`http://localhost:3000`)

1. **Login:** `admin` / `admin` (ou o que estiver no `.env`; o auto-cadastro está desativado).
2. O datasource **Prometheus** já vem **provisionado** (uid `prometheus`) — nada a configurar.
3. Menu **Dashboards → pasta `ShopFlow`**:

- **ShopFlow Overview** (`shopflow-overview`) — visão geral do sistema:
  *Services up*, *Orders placed*, *Saga outcomes*, *HTTP request rate*, *HTTP p95 latency*, *JVM heap
  used*, *Kafka consumer lag*, *Payments outcome*, *Reservations outcome*.
- **ShopFlow JVM (per service)** (`shopflow-jvm`) — saúde da JVM por serviço. Use o dropdown **Service**
  (variável `$application`) no topo para escolher o serviço: memória por área/pool, GC pause, threads
  (live/daemon), classes carregadas e CPU (process/system).

A janela de tempo já vem em `now-30m` e o refresh em `10s` (ajustáveis no canto superior direito).

> **Dica:** os painéis de negócio ficam em "No data" até existir tráfego — dispare alguns pedidos da
> seção 3 (*Gerar dados*) para vê-los reagir.

---

## 📇 Referência — métricas de negócio

Contadores customizados emitidos pela saga. No Prometheus, todo counter ganha o sufixo `_total`.

| Nome no código | Nome no Prometheus | Tag | Valores |
|---|---|---|---|
| `shopflow.orders.placed` | `shopflow_orders_placed_total` | — | — |
| `shopflow.saga.outcome` | `shopflow_saga_outcome_total` | `outcome` | `confirmed`, `rejected`, `cancelled` |
| `shopflow.payments.outcome` | `shopflow_payments_outcome_total` | `outcome` | `authorized`, `failed`, `refunded` |
| `shopflow.reservations.outcome` | `shopflow_reservations_outcome_total` | `outcome` | `reserved`, `failed`, `released` |

Além desses, todos os serviços expõem as métricas padrão do Micrometer/Actuator: `http_server_requests_*`
(latência e taxa por endpoint), `jvm_*` (memória, GC, threads, classes), `process_*`/`system_*` (CPU) e
as métricas de consumidor do Kafka (`kafka_consumer_*`).

---

## 🛠️ Solução de problemas

- **Alvo DOWN no Prometheus** → o serviço não está rodando, está em outra porta, ou ainda está
  inicializando. Confirme em *Status → Targets* e veja se o `bootRun` correspondente está de pé.
- **Painel de negócio vazio ("No data")** → ainda não houve pedidos; dispare a seção 3 (*Gerar dados*).
- **Prometheus não alcança os serviços** → eles precisam rodar no host nas portas esperadas;
  `host.docker.internal` resolve no Docker Desktop/Windows e, em Linux, o `extra_hosts: host-gateway` do
  `docker-compose.yml` garante o acesso do container ao host.
