# API

Referência da API REST do ShopFlow: endpoints, autenticação por rota, taxonomia de erros (RFC 7807) e
um *walkthrough* reproduzível do fluxo. Tudo entra pela borda — o **api-gateway** (`:8080`) — com um
**JWT** do Keycloak.

- [1. Endpoints](#1-endpoints)
- [2. Autenticação](#2-autenticação)
- [3. Erros (RFC 7807)](#3-erros-rfc-7807)
- [4. Walkthrough do fluxo](#4-walkthrough-do-fluxo)
- [5. OpenAPI / Swagger UI](#5-openapi--swagger-ui)

## 1. Endpoints

Base (via gateway): `http://localhost:8080`.

| Método | Path | Função | Exigência |
|---|---|---|---|
| `POST` | `/orders` | Cria o pedido (`PENDING`) e inicia a saga. | role **CUSTOMER** |
| `GET` | `/orders/{id}` | Estado e itens do pedido. | autenticado |
| `GET` | `/stock` | Lista o estoque. | role **ADMIN** |
| `GET` | `/stock/{productId}` | Estoque de um produto. | role **ADMIN** |
| `GET` | `/payments/{orderId}` | Pagamento de um pedido. | autenticado |
| `GET` | `/notifications` | Lista as notificações. | autenticado |
| `GET` | `/notifications/{orderId}` | Notificações de um pedido. | autenticado |

### 1.1 `POST /orders` — criar pedido

**Request body** (`PlaceOrderRequest`):

| Campo | Tipo | Validação | Descrição |
|---|---|---|---|
| `customerId` | `UUID` | `@NotNull` | Cliente dono do pedido. |
| `items` | `array` | `@NotEmpty` | Itens do pedido. |
| `items[].productId` | `UUID` | `@NotNull` | Produto. |
| `items[].quantity` | `int` | `@Positive` | Quantidade. |
| `items[].unitPrice` | `BigDecimal` | `@NotNull`, `@Positive` | Preço unitário (*snapshot* no request). |

```json
{
  "customerId": "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "items": [
    { "productId": "a1111111-1111-1111-1111-111111111111", "quantity": 2, "unitPrice": 400.00 }
  ]
}
```

**Response `201 Created`** (`OrderResponse`) — o pedido nasce `PENDING`; a saga roda em background:

```json
{
  "id": "9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f",
  "customerId": "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "status": "PENDING",
  "totalAmount": 800.00,
  "createdAt": "2026-06-22T12:34:56Z",
  "items": [
    { "id": "…", "productId": "a1111111-1111-1111-1111-111111111111", "quantity": 2, "unitPrice": 400.00 }
  ]
}
```

`totalAmount` é a soma de `quantity × unitPrice`. O `OrderStatus` evolui de forma assíncrona
(`PENDING → STOCK_RESERVED → PAID → CONFIRMED`, ou os terminais `REJECTED`/`CANCELLED`) — ver a
[máquina de estados](ARCHITECTURE.md#máquina-de-estados-do-pedido).

### 1.2 `GET /orders/{id}` — consultar pedido

Retorna o `OrderResponse` (mesmos campos acima) com o `status` atual. `404` (`order-not-found`) se não
existir.

### 1.3 `GET /stock` e `GET /stock/{productId}` — consultar estoque (ADMIN)

`StockResponse`: `productId` (UUID), `available` (int), `reserved` (int). `404` (`stock-item-not-found`)
para um `productId` inexistente.

### 1.4 `GET /payments/{orderId}` — consultar pagamento

`PaymentResponse`: `id` (UUID), `orderId` (UUID), `amount` (BigDecimal), `status`
(`AUTHORIZED` / `FAILED` / `REFUNDED`), `providerRef` (String). `404` (`payment-not-found`) se ainda não
houver pagamento para o pedido.

### 1.5 `GET /notifications` e `GET /notifications/{orderId}` — consultar notificações

`NotificationResponse`: `id` (UUID), `orderId` (UUID), `customerId` (UUID), `type`
(`ORDER_CREATED` / `ORDER_CONFIRMED` / `ORDER_CANCELLED` / `ORDER_REJECTED`), `message` (String),
`createdAt` (Instant).

## 2. Autenticação

Autenticação via **Keycloak** (realm `shopflow`, roles `CUSTOMER` / `ADMIN`). O **gateway** exige um JWT
válido na borda e **encaminha** o `Authorization` para os serviços, que atuam como **resource servers**
e aplicam as regras por endpoint (defesa em profundidade). As roles vêm do claim `realm_access.roles` do
JWT, mapeadas para `ROLE_*`. Sem token → **401**; token com role errada → **403**.

Obter um token (password grant, usuário de teste `customer` / `customer`):

```bash
TOKEN=$(curl -s \
  -d "client_id=shopflow-gateway" -d "client_secret=shopflow-gateway-secret" \
  -d "grant_type=password" -d "username=customer" -d "password=customer" \
  http://localhost:8088/realms/shopflow/protocol/openid-connect/token | jq -r .access_token)
```

Usuários de teste: `customer` / `customer` (CUSTOMER) e `manager` / `manager` (ADMIN). Detalhes
(inspecionar o JWT, exemplos por role) em [SECURITY.md](SECURITY.md).

## 3. Erros (RFC 7807)

Todas as respostas de erro seguem **Problem Details (RFC 7807)**. O `type` é montado a partir de
`https://wastecoder.com/problems` + o *slug* do `ProblemType`; a tradução exceção → status acontece
**só** no `GlobalExceptionHandler` de cada serviço. Ver
[ADR-0006](adr/0006-problem-details-rfc7807.md).

| Status | slug (`type`) | Serviço(s) | Quando ocorre |
|---|---|---|---|
| `400` | `validation-failed` | todos | Bean Validation no `@Valid` do request (inclui `errors[]`). |
| `400` | `invalid-request-parameter` | todos | Tipo de parâmetro inválido (ex.: `{id}` não-UUID). |
| `400` | `domain-error` | order / inventory / payment | Exceção de domínio não mapeada especificamente. |
| `404` | `order-not-found` | order | `GET /orders/{id}` sem pedido. |
| `404` | `stock-item-not-found` | inventory | `GET /stock/{productId}` sem produto. |
| `404` | `payment-not-found` | payment | `GET /payments/{orderId}` sem pagamento. |
| `500` | `internal-server-error` | todos | Falha inesperada (detalhe genérico; nunca ecoa a mensagem da exceção). |

> Falta de estoque (`InsufficientStockException`) **não** é um erro HTTP: é tratada como caminho normal
> da saga (pedido vai a `REJECTED`).

### Exemplo — pedido inexistente (404)

```json
{
  "type": "https://wastecoder.com/problems/order-not-found",
  "title": "Order not found",
  "status": 404,
  "detail": "Order not found: 9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f"
}
```

### Exemplo — validação (400 com `errors[]`)

```json
{
  "type": "https://wastecoder.com/problems/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "Validation failed: 1 error(s)",
  "errors": [
    { "field": "items", "message": "items must not be empty" }
  ]
}
```

## 4. Walkthrough do fluxo

Passo a passo reproduzível do **caminho feliz** (pedido → `CONFIRMED`), entrando pela borda
(`api-gateway :8080`). Estoque semeado no `inventory-service` (profile `seed`):

| productId | disponível |
|---|---|
| `a1111111-1111-1111-1111-111111111111` | 100 |
| `a2222222-2222-2222-2222-222222222222` | 50 |
| `a3333333-3333-3333-3333-333333333333` | 10 |
| `a4444444-4444-4444-4444-444444444444` | 0 |

```bash
# 1) Token de CUSTOMER no Keycloak (password grant)
TOKEN=$(curl -s \
  -d "client_id=shopflow-gateway" -d "client_secret=shopflow-gateway-secret" \
  -d "grant_type=password" -d "username=customer" -d "password=customer" \
  http://localhost:8088/realms/shopflow/protocol/openid-connect/token | jq -r .access_token)

# 2) Cria o pedido (total = 2 × 400.00 = 800.00, abaixo do limite do PSP)
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
        "customerId": "3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
        "items": [
          { "productId": "a1111111-1111-1111-1111-111111111111", "quantity": 2, "unitPrice": 400.00 }
        ]
      }'

# 3) Em ~1s a saga conclui. Consulte (use o id devolvido acima):
ID=9b2e7c10-3a4b-4c5d-8e6f-0a1b2c3d4e5f
curl -s http://localhost:8080/orders/$ID        -H "Authorization: Bearer $TOKEN"   # status: CONFIRMED
curl -s http://localhost:8080/payments/$ID      -H "Authorization: Bearer $TOKEN"   # status: AUTHORIZED
curl -s http://localhost:8080/notifications/$ID -H "Authorization: Bearer $TOKEN"   # ORDER_CREATED + ORDER_CONFIRMED
```

**Outros desfechos** (mude o corpo do pedido):

- **Cancelamento com compensação** — total ≥ 1000 faz o PSP recusar (regra do mock
  `MockPaymentGateway`, `decline-threshold = 1000.00`): ex. `quantity: 3` × `unitPrice: 400.00`
  (= 1200) → `PaymentFailed` → `ReleaseStock` → pedido `CANCELLED`.
- **Rejeição por falta de estoque** — peça o produto sem estoque
  (`productId: a4444444-4444-4444-4444-444444444444`) → `StockReservationFailed` → pedido `REJECTED`
  (sem pagamento, sem compensação).

> O trace ponta a ponta dessa saga aparece como um único trace no Zipkin — ver [OBSERVABILITY.md](OBSERVABILITY.md).

## 5. OpenAPI / Swagger UI

Cada serviço de domínio publica OpenAPI 3.0.3 (springdoc). Com a stack no ar:

| Serviço | Swagger UI | OpenAPI JSON |
|---|---|---|
| order-service | http://localhost:8081/swagger-ui.html | `:8081/v3/api-docs` |
| inventory-service | http://localhost:8082/swagger-ui.html | `:8082/v3/api-docs` |
| payment-service | http://localhost:8083/swagger-ui.html | `:8083/v3/api-docs` |
| notification-service | http://localhost:8084/swagger-ui.html | `:8084/v3/api-docs` |

Os caminhos `/swagger-ui/**` e `/v3/api-docs/**` são abertos (sem token) em cada serviço.
