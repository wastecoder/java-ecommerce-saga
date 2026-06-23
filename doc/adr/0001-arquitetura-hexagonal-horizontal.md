# ADR-0001: Arquitetura hexagonal (ports & adapters) horizontal

## Status

Aceito.

## Contexto

O projeto é avaliado, entre outros atributos, por **separação de conceitos**, **testabilidade** e
**manutenibilidade**. O layout padrão do Spring (`controller` / `service` / `repository`) tende a
misturar regra de negócio com framework: a lógica vaza para classes anotadas com `@Service`, o domínio
acaba dependendo de Spring, JPA e Kafka, e testar a regra exige subir contexto ou mockar
infraestrutura.

Cada serviço do ShopFlow é uma fatia de um **único** contexto (pedidos, estoque, pagamento,
notificação) com invariantes próprias — vale isolar essa lógica do mecanismo de persistência, do
transporte HTTP e do broker de mensagens.

## Decisão

Adotar **arquitetura hexagonal (ports & adapters)** com **layout horizontal** em cada serviço, sob a
raiz `com.wastecoder.shopflow.<service>`:

- `domain/` — `model/` e `exception/`. **Java puro**: sem Spring, sem JPA, sem Kafka.
- `application/` — casos de uso (`usecase/`, incluindo o coordenador da saga), **ports** de entrada
  (`port/in/` — `*UseCase`) e de saída (`port/out/` — `*Repository`, `*Publisher`) e `viewmodel/`
  (`*Command`/`*Result`).
- `adapter/` — `web/` (REST + Problem Details), `messaging/in/` e `messaging/out/` (Kafka),
  `persistence/` (JPA) e `config/`.

**Regra de dependência:** sempre apontando para dentro — `adapter → application → domain`. O domínio
não conhece HTTP, banco nem broker.

**A convenção de nomes carrega o papel arquitetural** (`*UseCase`, `*UseCaseImpl`, `*Repository`,
`*RepositoryImpl`, `*Entity`, `*EntityMapper`, `*Request`/`*Response`, `*Listener`, `Kafka*Publisher`)
— ver a tabela em [ARCHITECTURE.md](../ARCHITECTURE.md).

## Alternativas consideradas

- **MVC flat (`controller`/`service`/`repository`).** Rejeitada: mistura regra de negócio com
  framework; domínio acoplado a Spring/JPA/Kafka; pior testabilidade.
- **Hexagonal por feature module (vertical).** Não adotada: cada microsserviço já é uma fatia de um
  único domínio; fatiar por feature geraria aninhamento redundante. O layout horizontal é mais simples.

## Consequências

**Positivas:**
- Domínio é Java puro, testável sem contexto Spring.
- Trocar um adapter (outro banco, outro broker) não toca a regra de negócio.
- Ports explícitos facilitam mockar nos testes de caso de uso.

**Negativas / custos:**
- Mais arquivos e indireções (ports + impls + mappers) do que o MVC flat.
- A convenção de sufixos vira um contrato que precisa ser respeitado em todos os serviços.
