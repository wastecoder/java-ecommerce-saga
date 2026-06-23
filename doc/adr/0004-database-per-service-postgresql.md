# ADR-0004: Database-per-service com PostgreSQL

## Status

Aceito.

## Contexto

Em uma arquitetura de microsserviços, compartilhar um único banco entre serviços acopla os schemas e
mina a autonomia: uma mudança no modelo de um serviço pode quebrar outro, e ninguém é dono claro dos
dados. O ShopFlow quer serviços **autônomos**, cada um dono do seu estado.

## Decisão

Cada serviço de domínio tem o **seu próprio banco** no PostgreSQL — `orderdb`, `inventorydb`,
`paymentdb`, `notificationdb` (criados pelo `infra/init-db.sql`). Nenhum serviço acessa o banco de
outro. Relações **entre** serviços acontecem **por eventos** (Kafka), não por chave estrangeira nem
join. Schema versionado por **Flyway** em cada serviço (`ddl-auto: validate`).

## Alternativas consideradas

- **Banco compartilhado (schema único).** Rejeitada: acopla os serviços pelo banco, dificulta evoluir
  schemas de forma independente e dilui a propriedade dos dados.

## Consequências

**Positivas:**
- Isolamento de dados e autonomia de schema/evolução por serviço.
- Cada serviço pode escalar e versionar seu banco independentemente.

**Negativas / custos:**
- **Sem joins** entre serviços; consultas que cruzam contextos viram troca de eventos.
- Consistência é **eventual**, garantida pela saga e pelos eventos (ver [ADR-0002](0002-saga-orquestrada.md)).
- Mais instâncias lógicas de banco para operar.
