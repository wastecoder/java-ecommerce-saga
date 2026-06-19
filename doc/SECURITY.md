# 🔐 Segurança — ShopFlow (Keycloak)

Guia prático de como **operar** a camada de identidade do ShopFlow: subir o Keycloak, conhecer o
realm `shopflow`, obter um **JWT** e inspecionar suas roles.

A autenticação/autorização usa **Keycloak** (OAuth2/OIDC). O Keycloak é o **emissor de tokens**: os
clientes (simulados — Postman, `curl`, testes) trocam credenciais por um **access token JWT**, que
carrega as roles do usuário. O **gateway** e os **4 serviços de domínio** são *resource servers* que
**validam o JWT** (assinatura via JWKS + issuer): o gateway autentica na borda e os serviços enforçam as
**roles** por endpoint (seção 6).

> **Modelo de execução:** o Keycloak roda em **container** (`docker compose`), enquanto os serviços
> Spring rodam no **host** via `bootRun` (igual à stack de observabilidade). Por isso o issuer dos
> tokens é `http://localhost:8088/realms/shopflow` — todos acessam o Keycloak por `localhost:8088`.
>
> **Storage efêmero:** o container sobe em modo `start-dev` (H2 em memória) e **re-importa o realm a
> cada start** a partir de `infra/keycloak/realm-export.json`. Esse arquivo é a **fonte da verdade** —
> mudanças feitas à mão no admin console se perdem no restart (mesma filosofia do log do Kafka e do
> Zipkin).
>
> **Pré-requisitos:** Docker Desktop em execução.

---

## 🐳 1. Subir o Keycloak

Na raiz do repositório:

```bash
docker compose up -d keycloak
```

As portas e credenciais saem do `.env` (com defaults caso a variável não exista):

| Variável | Default | Para quê |
|---|---|---|
| `KEYCLOAK_PORT` | `8088` | Porta do Keycloak no host |
| `KEYCLOAK_ADMIN` | `admin` | Usuário admin do **realm master** (só para o admin console) |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Senha do admin do realm master |

> As variáveis `KEYCLOAK_ADMIN*` mapeiam para `KC_BOOTSTRAP_ADMIN_*` no `docker-compose.yml` — os nomes
> foram **renomeados no Keycloak 26**; mantemos os nomes amigáveis no `.env`.

Conferir que subiu e importou o realm:

```bash
docker compose ps keycloak
docker logs shopflow-keycloak | grep -i "imported"
# -> ... Realm 'shopflow' imported
```

Parar: `docker compose down` (o realm é re-importado na próxima subida, então nada se perde).

---

## 🖥️ 2. Admin console

Abra **`http://localhost:8088`** → **Administration Console** → login `admin` / `admin`.

No canto superior esquerdo, troque o realm de **`master`** para **`shopflow`** para ver roles, client e
usuários abaixo.

> O `admin`/`admin` é o admin do realm **master** (gestão do Keycloak), **não** um usuário do
> ShopFlow. Os usuários da aplicação ficam no realm `shopflow` (próxima seção).

---

## 🧾 3. O realm `shopflow`

Tudo abaixo nasce do `infra/keycloak/realm-export.json`.

**Roles (realm roles):**

| Role | Significado |
|---|---|
| `CUSTOMER` | Usuário final que faz e acompanha pedidos |
| `ADMIN` | Operador de back-office com acesso total |

**Client (OAuth2):**

| Atributo | Valor |
|---|---|
| `clientId` | `shopflow-gateway` |
| Tipo | **Confidencial** (`publicClient: false`) |
| `secret` | `shopflow-gateway-secret` ⚠️ **dev-only** |
| Direct Access Grants | **habilitado** (permite o fluxo *password* para Postman/`curl`/testes) |
| Standard/Implicit flow | desabilitados |

**Usuários de teste:**

| Usuário | Senha | Role |
|---|---|---|
| `customer` | `customer` | `CUSTOMER` |
| `manager` | `manager` | `ADMIN` |

---

## 🔑 4. Obter um JWT (fluxo *password*)

O client `shopflow-gateway` tem **Direct Access Grants**, então dá para trocar usuário+senha por um
token direto no *token endpoint* (sem navegador):

```
POST http://localhost:8088/realms/shopflow/protocol/openid-connect/token
```

```bash
curl -s \
  -d "client_id=shopflow-gateway" \
  -d "client_secret=shopflow-gateway-secret" \
  -d "grant_type=password" \
  -d "username=customer" \
  -d "password=customer" \
  http://localhost:8088/realms/shopflow/protocol/openid-connect/token
```

A resposta (HTTP 200) traz `access_token`, `refresh_token`, `expires_in` etc. Troque
`username`/`password` por `manager`/`manager` para um token com a role `ADMIN`.

> Esse `access_token` vai no header `Authorization: Bearer <token>` das chamadas aos endpoints
> protegidos (direto no serviço ou via gateway) — ver seção 6.

---

## 🔍 5. Inspecionar o token

Um JWT são três partes Base64URL separadas por `.` — o **payload** (a do meio) carrega as roles em
`realm_access.roles`. Cole o token em <https://jwt.io> ou decodifique na linha de comando:

```bash
# extrai o access_token e imprime as roles do realm (precisa de python no PATH)
TOKEN=$(curl -s -d "client_id=shopflow-gateway" -d "client_secret=shopflow-gateway-secret" \
  -d "grant_type=password" -d "username=customer" -d "password=customer" \
  http://localhost:8088/realms/shopflow/protocol/openid-connect/token \
  | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

echo "$TOKEN" | python -c "import sys,base64,json; p=sys.stdin.read().split('.')[1]; p+='='*(-len(p)%4); print(json.dumps(json.loads(base64.urlsafe_b64decode(p)), indent=2))"
```

Claims relevantes esperados:

```json
{
  "iss": "http://localhost:8088/realms/shopflow",
  "azp": "shopflow-gateway",
  "preferred_username": "customer",
  "realm_access": { "roles": ["CUSTOMER"] }
}
```

---

## 🧭 6. Endpoints protegidos (resource servers + gateway)

Cada serviço (e o gateway) é um **OAuth2 resource server**: configurado com o `issuer-uri`, ele baixa as
chaves públicas do realm (do `jwks_uri` anunciado em
`http://localhost:8088/realms/shopflow/.well-known/openid-configuration`) e **valida a assinatura + o
issuer** de cada `Bearer` token. Config (em cada `application.yaml`):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8088/realms/shopflow}
```

> **Ordem de subida:** o Keycloak precisa estar de pé **antes** dos serviços — o `issuer-uri` é
> resolvido (descoberta OIDC) no startup. Suba `docker compose up -d keycloak` antes dos `bootRun`.

**Divisão de responsabilidades:** o **gateway** (resource server reativo) exige um token válido em toda
rota de negócio e **encaminha** o header `Authorization` para o serviço a jusante; cada **serviço**
revalida o token e **enforça a role** do endpoint (defense in depth). As roles do Keycloak chegam em
`realm_access.roles` e são mapeadas para autoridades `ROLE_*` (`KeycloakRealmRoleConverter`).

### Política role → endpoint

| Método / rota | Regra |
|---|---|
| `POST /orders` | role **CUSTOMER** |
| `GET /orders/**` | autenticado (qualquer JWT válido) |
| `GET /stock/**` | role **ADMIN** |
| `GET /payments/**` | autenticado |
| `GET /notifications/**` | autenticado |
| `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**` | **aberto** (permitAll) |

> Rate limit do gateway **não** faz parte deste item (não está na Fase 6). Os endpoints `/actuator/**`
> ficam abertos de propósito (senão o Prometheus não conseguiria raspá-los).

### Chamar um endpoint protegido

Obtenha um token (seção 4) e mande no header `Authorization: Bearer <token>`. Direto no serviço **ou**
via gateway (`:8080`, que roteia para o serviço pelo Eureka):

```bash
TOKEN=$(curl -s -d "client_id=shopflow-gateway" -d "client_secret=shopflow-gateway-secret" \
  -d "grant_type=password" -d "username=customer" -d "password=customer" \
  http://localhost:8088/realms/shopflow/protocol/openid-connect/token \
  | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

# Via gateway (entrada única) — CUSTOMER cria pedido:
curl -i -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"customerId":"3f8b1c2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d","items":[{"productId":"a1111111-1111-1111-1111-111111111111","quantity":2,"unitPrice":400.00}]}'
# -> 201 Created

# Sem token -> 401 Unauthorized:
curl -i -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{}'

# Token sem a role exigida -> 403 Forbidden (ex.: token de 'manager'/ADMIN no POST /orders, que pede CUSTOMER):
# GET /stock exige ADMIN: use um token de 'manager'.
curl -i http://localhost:8082/stock -H "Authorization: Bearer $TOKEN"   # token CUSTOMER -> 403
```

Resumo das respostas: **sem token → 401**; **token válido mas role errada → 403**; **token válido com a
role certa → 2xx**. `/actuator/health` e `/swagger-ui.html` seguem abertos (sem token).

---

## 🛠️ Solução de problemas

- **`docker logs` não mostra "Realm 'shopflow' imported"** → confira o mount do volume
  (`./infra/keycloak/realm-export.json`) e se o JSON é válido. O import só roda por causa do
  `--import-realm` no `command`.
- **Token endpoint responde `401 invalid_client`** → o client é **confidencial**; envie também
  `client_secret=shopflow-gateway-secret`.
- **`400 unauthorized_client` / "Direct access grants disabled"** → o realm importado não está com o
  client esperado (talvez um realm antigo persistido). Como o storage é efêmero, um `docker compose
  down` + `up -d keycloak` reimporta do zero.
- **`invalid_grant` (Invalid user credentials)** → usuário/senha errados; use `customer`/`customer` ou
  `manager`/`manager`.
- **Porta 8088 ocupada** → ajuste `KEYCLOAK_PORT` no `.env` (lembre que o issuer dos tokens passa a
  refletir a nova porta).
- **Serviço não sobe / erro de `JwtDecoder` no startup** → o `issuer-uri` aponta para um Keycloak fora
  do ar. Suba o Keycloak **antes** dos serviços (a descoberta OIDC é no startup).
- **Sempre 401, mesmo com token** → token expirado, ou o `iss` do token não bate com o `issuer-uri` do
  serviço (ex.: token tirado de outra porta/host). Gere um token novo do mesmo `localhost:8088`.
- **403 inesperado** → o token é válido mas não tem a role exigida pelo endpoint (ver a tabela da seção
  6). Use `manager` para rotas **ADMIN** (`/stock/**`) e `customer` para `POST /orders` (**CUSTOMER**).

> ⚠️ **Dev-only:** o `secret` do client e as senhas dos usuários estão em texto puro no
> `realm-export.json` **de propósito**, para o ambiente local subir sem fricção. Não reutilize nada
> disso fora do desenvolvimento.
