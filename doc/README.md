# Documentação

Esta pasta concentra a documentação técnica do **ShopFlow**. O [README da raiz](../README.md) é o ponto
de entrada e um resumo; aqui está o detalhe. Documentação em **português**; código em **inglês**.

## Documentos

| Documento | Para quê serve |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Arquitetura hexagonal, estrutura de pacotes, fluxo da SAGA (sequência + máquina de estados), tópicos/contratos Kafka e modelo de domínio. |
| [API.md](API.md) | Referência da API REST: endpoints, autenticação por rota, taxonomia de erros (RFC 7807) e o *walkthrough* do fluxo com `curl`. |
| [postman/](../postman/README.md) | Coleção Postman (importável) + script HTTPie para exercitar a API ponta a ponta, com instruções de uso. |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Como rodar localmente, comandos Gradle, URLs úteis, profiles/seed, Docker e CI/CD (GHCR). |
| [TESTS.md](TESTS.md) | Estratégia de testes, Object Mother, estilo de teste, Testcontainers e gates de JaCoCo/Pitest. |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Métricas (Prometheus/Grafana) e tracing distribuído (Zipkin), inclusive pelo Kafka. |
| [SECURITY.md](SECURITY.md) | Keycloak, OAuth2/JWT, regras por endpoint e exemplos de uso. |
| [CHALLENGE.md](CHALLENGE.md) | O *brief* do desafio: contexto, requisitos e critérios de aceite. |
| [PROGRESS.md](PROGRESS.md) | Roadmap do MVP, fase a fase (estado atual). |
| [FUTURE-PROGRESS.md](FUTURE-PROGRESS.md) | Visão pós-MVP ("Completo"). |

## ADRs

Architecture Decision Records — o *porquê* de cada decisão estrutural.

| ADR | Tema |
|---|---|
| [0001](adr/0001-arquitetura-hexagonal-horizontal.md) | Arquitetura hexagonal (ports & adapters) horizontal |
| [0002](adr/0002-saga-orquestrada.md) | SAGA orquestrada (orchestration), não coreografia |
| [0003](adr/0003-kafka-spring-kafka-kraft.md) | Kafka via Spring Kafka direto, em modo KRaft |
| [0004](adr/0004-database-per-service-postgresql.md) | Database-per-service com PostgreSQL |
| [0005](adr/0005-idempotencia-e-dlt.md) | Consumidores idempotentes + Dead Letter Topic |
| [0006](adr/0006-problem-details-rfc7807.md) | Tratamento de erros com Problem Details (RFC 7807) |

## Por onde começar

- Quero **entender a arquitetura / a SAGA** → [ARCHITECTURE.md](ARCHITECTURE.md)
- Quero **usar a API** (endpoints, exemplos, erros) → [API.md](API.md)
- Quero **uma coleção pronta** (Postman/HTTPie) → [postman/](../postman/README.md)
- Quero **rodar localmente** → [DEVELOPMENT.md](DEVELOPMENT.md)
- Quero **escrever ou rodar testes** → [TESTS.md](TESTS.md)
- Quero **ver métricas e tracing** → [OBSERVABILITY.md](OBSERVABILITY.md)
- Quero **entender a segurança (auth)** → [SECURITY.md](SECURITY.md)
- Quero **entender o porquê das decisões** → [ADRs](#adrs)
- Quero **o enunciado do desafio** → [CHALLENGE.md](CHALLENGE.md)
- Quero **acompanhar o progresso** → [PROGRESS.md](PROGRESS.md) · [FUTURE-PROGRESS.md](FUTURE-PROGRESS.md)

## Convenções de manutenção

- **Mermaid sempre que possível** — diagramas viram código versionável e revisável em diff.
- **Tabelas em vez de prosa** ao enumerar campos, comandos, status, tópicos ou erros.
- **Linkar entre docs** quando um conceito aparece em mais de um lugar, em vez de duplicar.
- **Adicionar um ADR antes de mudar uma decisão estrutural** — não silenciosamente. Ao substituir,
  datar o status do ADR antigo.
- **Documentação em português; código em inglês.**
