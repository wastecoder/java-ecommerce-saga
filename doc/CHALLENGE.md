# 🛒 Desafio — ShopFlow (E-commerce orientado a eventos com SAGA)

Backend de e-commerce que processa pedidos através de uma **SAGA orquestrada** sobre **Apache Kafka**,
com **Spring Cloud** (Gateway + Eureka + Resilience4j), arquitetura **hexagonal (horizontal)**,
observabilidade, segurança OAuth2/JWT e CI/CD. Projeto de estudo/portfólio — **back-end puro**.

> Este documento é o **brief** do desafio (o *quê*). O *como* está nos docs vivos: arquitetura, fluxo da
> SAGA e contratos de evento em [`ARCHITECTURE.md`](ARCHITECTURE.md); decisões em [`adr/`](README.md#adrs);
> API em [`API.md`](API.md); execução em [`DEVELOPMENT.md`](DEVELOPMENT.md); testes em
> [`TESTS.md`](TESTS.md). Índice geral em [`README.md`](README.md).

## 1. Contexto e objetivo
Quando um cliente cria um pedido, o sistema precisa **reservar estoque**, **cobrar o pagamento** e
**confirmar (ou desfazer)** a operação de forma consistente entre serviços independentes, sem
transação distribuída tradicional (2PC). Esse é o problema clássico que a **SAGA** resolve — o
coração deste desafio.

Objetivo de aprendizado: Kafka (produtores/consumidores, idempotência, DLQ), Spring Cloud (Gateway,
discovery, circuit breaker), Docker/Compose e CI/CD.

## 2. O que construir (requisitos funcionais)
1. **Criar pedido** — `POST /orders` cria o pedido (status `PENDING`) e inicia a saga.
2. **Reservar estoque** — o `inventory-service` reserva os itens; se faltar, responde falha.
3. **Processar pagamento** — o `payment-service` autoriza o pagamento (PSP mockado, com cenários de
   falha controláveis).
4. **Confirmar ou compensar** — pagamento aprovado → `CONFIRMED`; pagamento recusado → `CANCELLED`
   com **compensação** (liberar estoque); sem estoque → `REJECTED`.
5. **Notificar** — o `notification-service` consome `order.events` e "envia" notificação (mock/log).
6. **Consultar pedido** — `GET /orders/{id}` retorna estado e itens.

## 3. Requisitos técnicos
- Comunicação **assíncrona via Kafka** (event-driven); saga **orquestrada** pelo `order-service`.
- **Idempotência** nos consumidores e **DLQ** por consumidor.
- **Hexagonal horizontal** por serviço; domínio sem dependência de framework.
- **Docker Compose** sobe todo o ambiente; **CI/CD** no GitHub Actions.
- **Observabilidade** (métricas + tracing) e **segurança** (OAuth2/JWT) no gateway.
- **Testes** unitários e de integração (Testcontainers) com gates de cobertura.

## 4. Critérios de aceite
- [ ] `POST /orders` com itens disponíveis → pedido **CONFIRMED**; cadeia de eventos visível no Kafka UI.
- [ ] `POST /orders` sem estoque → pedido **REJECTED**; nenhum pagamento criado.
- [ ] Pagamento recusado → pedido **CANCELLED** + `ReleaseStock` compensa (estoque volta ao valor original).
- [ ] Reprocessar a mesma mensagem **não** altera o resultado (idempotência).
- [ ] Mensagem inválida vai para o **DLT** após os retries.
- [ ] `notification-service` registra notificação em confirm **e** cancel.
- [ ] **Trace distribuído** mostra a saga ponta-a-ponta no Zipkin.
- [ ] Endpoints protegidos exigem **JWT** válido (Keycloak).
- [ ] `./gradlew check` verde (unit + integração Testcontainers + gates JaCoCo/Pitest).
- [ ] Pipeline GitHub Actions verde + imagens publicadas no GHCR.
