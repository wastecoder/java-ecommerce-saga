# Testes

Estratégia, padrões e ferramentas de qualidade do ShopFlow.

- [1. Estratégia](#1-estratégia)
- [2. Estrutura de pastas](#2-estrutura-de-pastas)
- [3. Object Mother](#3-object-mother)
- [4. Estilo de teste](#4-estilo-de-teste)
- [5. Testes de integração](#5-testes-de-integração)
- [6. JaCoCo (cobertura)](#6-jacoco-cobertura)
- [7. Pitest (mutation testing)](#7-pitest-mutation-testing)
- [8. Como adicionar um teste novo](#8-como-adicionar-um-teste-novo)

## 1. Estratégia

Duas naturezas de teste por serviço, separadas por sufixo de classe, mais um módulo de teste
ponta-a-ponta:

| Tipo | Sufixo / módulo | Ferramentas | Roda em |
|---|---|---|---|
| Unitário | `*Test` | JUnit 5 + AssertJ + Mockito | `./gradlew test` |
| Integração | `*IntegrationTest` | JUnit 5 + Testcontainers (Kafka + PostgreSQL [+ Keycloak]) | `./gradlew integrationTest` |
| Ponta-a-ponta | módulo `integration-tests` | sobe os 4 serviços contra Kafka/PostgreSQL/Keycloak reais | `./gradlew integrationTest` |

A task `test` **exclui** `*IntegrationTest`; a `integrationTest` os inclui e roda depois
(`shouldRunAfter("test")`). O `check` depende de ambas + da verificação de cobertura + do `pitest`.

## 2. Estrutura de pastas

`src/test` espelha `src/main`, mais o pacote `testsupport`:

```
<service>/src/test/java/com/wastecoder/shopflow/<service>/
├── domain/                  # testes de modelo/regras (unit)
├── application/usecase/     # testes de casos de uso (unit, ports mockados)
├── adapter/
│   ├── web/                 # *ControllerTest (unit) + *ControllerIntegrationTest
│   ├── messaging/           # round-trip de envelope, listeners (integração)
│   └── persistence/         # *RepositoryImplIntegrationTest
├── testsupport/mother/      # Object Mothers
└── TestcontainersConfiguration   # beans de Kafka/PostgreSQL container (reutilizável)
```

O teste ponta-a-ponta fica no módulo dedicado `integration-tests`
(`com.wastecoder.shopflow.e2e.OrderSagaEndToEndIntegrationTest`).

## 3. Object Mother

Fixtures **sempre** via padrão **Object Mother** — nunca construção ad-hoc inline de objetos de
domínio, commands ou payloads. As Mothers vivem em `testsupport/mother/`.

Regras:

- Cada Mother é uma classe `final` com **construtor privado** e só **métodos estáticos**.
- Métodos nomeados pelo **cenário** que produzem (ex.: `OrderMother.aPendingOrder()`).
- **Sem** builders `with*` nem API fluente. Precisa de um cenário novo? Adicione um método novo.

Exemplos: `OrderMother`, `StockItemMother`, `PaymentCommandMother`, `NotificationMother`.

## 4. Estilo de teste

- `@ExtendWith(MockitoExtension.class)` com campos `@Mock` nos testes unitários de caso de uso.
- Todo `@Test` com `@DisplayName` no formato **Given/When/Then** e corpo estruturado com comentários
  `// Given`, `// When`, `// Then`. Exemplo:

  ```java
  @Test
  @DisplayName("Given a token with realm_access.roles, when converting, then it maps each role to a ROLE_ authority")
  void convert_mapsRealmRolesToAuthorities() {
      // Given ...
      // When ...
      // Then ...
  }
  ```

- Testes web com `MockMvc` verificam o status/resposta (`andExpect(status()...)`) — o
  `@RestControllerAdvice` traduz a exceção em resposta HTTP.

## 5. Testes de integração

- Usam `@SpringBootTest` (ou fatias) com Kafka e PostgreSQL **reais** via `TestcontainersConfiguration`
  (beans `KafkaContainer` `apache/kafka-native` e `PostgreSQLContainer`), importada com
  `@Import(TestcontainersConfiguration.class)`.
- Cobrem o que mocks não garantem: o round-trip de serialização do `EventEnvelope` pelo Kafka, a
  persistência JPA, o fluxo HTTP completo dos controllers e a saga.
- O **teste ponta-a-ponta** (`OrderSagaEndToEndIntegrationTest`) sobe os quatro serviços como contextos
  Spring independentes num único JVM, comunicando-se por um **Kafka real**, com **PostgreSQL** (um banco
  por serviço) e um **Keycloak real** (realm `shopflow` importado) emitindo JWTs de verdade — e exercita
  o caminho feliz e a compensação ponta a ponta.
- Docker precisa estar rodando.

## 6. JaCoCo (cobertura)

Gate ligado ao `check` (`jacocoTestCoverageVerification`), agregando os `.exec` de `test` e
`integrationTest`:

| Counter | Mínimo |
|---|---|
| `LINE` | 85% |
| `BRANCH` | 75% |

Excludes (sem lógica a cobrir): `**/*Application.class`, `**/adapter/web/dto/**`.

```bash
./gradlew jacocoTestReport     # relatório HTML em build/reports/jacoco
./gradlew check                # roda o gate (falha se abaixo do mínimo)
```

## 7. Pitest (mutation testing)

- Versão `1.25.4` (plugin JUnit5 `1.2.3`), mutators **STRONGER**.
- Alvos por serviço: `...<service>.application.*` e `...<service>.adapter.web.*Controller`.
- Thresholds: **mutação 80%**, **cobertura 80%**. Exclui `*IntegrationTest` (não roda em JVM forkado).

```bash
./gradlew pitest               # relatório em build/reports/pitest
```

## 8. Como adicionar um teste novo

1. Escolha a natureza: lógica isolada → unitário (`*Test`); algo que toca Kafka/banco → integração
   (`*IntegrationTest`).
2. Reaproveite/estenda uma **Mother** para o cenário; se faltar, adicione um método estático nomeado a
   ela (não construa inline).
3. Siga o estilo da [seção 4](#4-estilo-de-teste): `@DisplayName` Given/When/Then.
4. Caso de uso → mocke os ports out. Controller → `MockMvc`. Mensageria/persistência → integração com
   Testcontainers.
5. Rode `./gradlew test` (ou `integrationTest`) e confira os gates com `./gradlew check`.
