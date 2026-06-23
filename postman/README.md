# Coleção Postman / HTTPie — ShopFlow

Artefatos para exercitar a API do ShopFlow ponta a ponta, entrando pela borda
(**api-gateway** `:8080`) com **JWT** do Keycloak:

| Arquivo | O que é |
|---|---|
| `ShopFlow.postman_collection.json` | Coleção Postman v2.1 (12 requests: token, pedidos, estoque, pagamentos, notificações). |
| `ShopFlow.postman_environment.json` | Environment "ShopFlow (dev)" com URLs, credenciais e variáveis de runtime. |
| `httpie-flow.sh` | O caminho feliz da SAGA em HTTPie (+ ponteiros para os outros desfechos). |

> **Pré-requisitos:** a stack precisa estar no ar — infra via `docker compose up -d` e os 6 serviços
> via `bootRun` (ordem e perfis em [`../doc/DEVELOPMENT.md`](../doc/DEVELOPMENT.md)). Para o Postman,
> basta o app/Postman web; para o HTTPie, instale `httpie` e `jq`.

## Postman

### Importar
1. **Import** → selecione `ShopFlow.postman_collection.json` e `ShopFlow.postman_environment.json`.
2. No canto superior direito, selecione a environment **"ShopFlow (dev)"**.

### Rodar
1. `Auth → Obter token (customer)` — faz o *password grant* e salva o JWT em `{{access_token}}`.
   As demais requests herdam `Bearer {{access_token}}` automaticamente.
2. `Orders → Criar pedido — caminho feliz` — cria o pedido (nasce `PENDING`) e salva o id em
   `{{order_id}}`.
3. Aguarde ~1s (a SAGA é assíncrona) e rode os GETs: `Consultar pedido`, `Pagamento por pedido`,
   `Notificações por pedido`.
4. Para o estoque (role **ADMIN**), rode antes `Auth → Obter token (manager)` (salva `{{admin_token}}`);
   o folder **Stock** já usa esse token.

### Desfechos da SAGA

| Request | Pedido | Resultado |
|---|---|---|
| `Criar pedido — caminho feliz` | 2 × 400.00 = 800.00 | `CONFIRMED` (pagamento `AUTHORIZED`) |
| `Criar pedido — cancela` | 3 × 400.00 = 1200.00 (≥ limite do PSP) | `CANCELLED` (pagamento `FAILED` + compensação) |
| `Criar pedido — rejeita` | produto `a4444444-…` (sem estoque) | `REJECTED` (sem pagamento) |

As variantes salvam o id em `{{cancel_order_id}}` / `{{reject_order_id}}` — consulte
`GET /orders/{{cancel_order_id}}` e `GET /orders/{{reject_order_id}}` para ver os status.

### Erros (segurança)
- `Orders → Criar pedido — sem token` → **401** (a borda exige JWT).
- `Stock` com um token de **CUSTOMER** → **403** (estoque é role **ADMIN**). Para reproduzir, aponte o
  Bearer do folder Stock para `{{access_token}}` em vez de `{{admin_token}}`.

## HTTPie

Mesmo fluxo no terminal. Com a stack no ar:

```bash
# Token de CUSTOMER (password grant)
TOKEN=$(http --form POST http://localhost:8088/realms/shopflow/protocol/openid-connect/token \
  client_id=shopflow-gateway client_secret=shopflow-gateway-secret \
  grant_type=password username=customer password=customer | jq -r .access_token)

# Cria o pedido (caminho feliz) e guarda o id
ORDER_ID=$(http POST http://localhost:8080/orders "Authorization:Bearer $TOKEN" \
  customerId=3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d \
  items:='[{"productId":"a1111111-1111-1111-1111-111111111111","quantity":2,"unitPrice":400.00}]' \
  | jq -r .id)

# Consulta (após ~1s a saga conclui)
http GET http://localhost:8080/orders/$ORDER_ID        "Authorization:Bearer $TOKEN"   # CONFIRMED
http GET http://localhost:8080/payments/$ORDER_ID      "Authorization:Bearer $TOKEN"   # AUTHORIZED
http GET http://localhost:8080/notifications/$ORDER_ID "Authorization:Bearer $TOKEN"   # ORDER_CREATED + ORDER_CONFIRMED
```

Ou rode o script pronto (aceita `GATEWAY`/`KEYCLOAK` por variável de ambiente):

```bash
./httpie-flow.sh
# ou apontando para outros hosts:
GATEWAY=http://localhost:8080 KEYCLOAK=http://localhost:8088 ./httpie-flow.sh
```

Os comentários no fim do `httpie-flow.sh` mostram como obter os desfechos **cancela**/**rejeita** e o
token de **manager** para `GET /stock`.

> Referência completa da API (endpoints, erros RFC 7807, walkthrough): [`../doc/API.md`](../doc/API.md).
