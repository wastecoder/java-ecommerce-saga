# 🔐 Segurança — ShopFlow (Keycloak)

Guia prático de como **operar** a camada de identidade do ShopFlow: subir o Keycloak, conhecer o
realm `shopflow`, obter um **JWT** e inspecionar suas roles.

A autenticação/autorização usa **Keycloak** (OAuth2/OIDC). O Keycloak é o **emissor de tokens**: os
clientes (simulados — Postman, `curl`, testes) trocam credenciais por um **access token JWT**, que
carrega as roles do usuário. A validação desse token no gateway e nos serviços (resource servers) é o
**item 2 da Fase 6** — ainda **não** está ligada; por enquanto os endpoints REST continuam abertos.

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

> No item 2, esse `access_token` vai no header `Authorization: Bearer <token>` das chamadas ao gateway.

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

## 🧭 6. O que o item 2 vai consumir

A configuração OIDC do realm fica em:

```
http://localhost:8088/realms/shopflow/.well-known/openid-configuration
```

Esse documento expõe o `issuer`, o `jwks_uri` (chaves públicas para validar a assinatura) e os
endpoints de token. No **item 2**, o gateway e os serviços (como *resource servers*) vão apontar o
`issuer-uri` para `http://localhost:8088/realms/shopflow` e validar o `Bearer` token a partir do JWKS —
fechando o critério da fase (`POST /orders` exige JWT válido).

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

> ⚠️ **Dev-only:** o `secret` do client e as senhas dos usuários estão em texto puro no
> `realm-export.json` **de propósito**, para o ambiente local subir sem fricção. Não reutilize nada
> disso fora do desenvolvimento.
