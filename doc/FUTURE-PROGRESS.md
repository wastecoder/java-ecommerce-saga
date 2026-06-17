# 🔮 Próximos passos — Evolução futura (visão "Completo")

Ideias para evoluir o ShopFlow **depois** do MVP+Notification. Mesmo formato do `PROGRESS.md`
(ao concluir: `- [x] ~~item~~`). Nada aqui é necessário para o portfólio inicial.

## 🗂️ Fase 9 — Configuração centralizada
- [ ] Spring Cloud Config Server + refresh dinâmico (`@RefreshScope`)
- **Critério:** alterar config sem rebuild dos serviços.

## 🧾 Fase 10 — Confiabilidade de eventos
- [ ] **Transactional Outbox** + Debezium/CDC (publicação atômica DB+broker)
- [ ] **Schema Registry** + Avro/Protobuf (contratos versionados no lugar de JSON)
- **Critério:** zero perda/duplicação de evento sob falha; contratos evoluíveis.

## 🧱 Fase 11 — Novos bounded contexts
- [ ] `catalog-service`, `shipping-service`, `customer-service`
- [ ] `order-service`: snapshot dos atributos do produto na linha do pedido (`productName`,
      `sku`, `currency`) lidos do `catalog-service` na compra — congela o registro histórico
      (imune a renomear/reajustar/excluir no catálogo) e remove o `unitPrice`/nome vindos do
      cliente; fecha o *trust gap* da "Decisão de preço" (`PROGRESS.md`, Fase 1)
- [ ] Saga mais longa (inclui envio) com `RefundPayment` como compensação
- **Critério:** saga multi-etapas com compensações encadeadas.

## 📊 Fase 12 — Stream processing / CQRS
- [ ] **Kafka Streams** para projeções/relatórios (ex.: vendas por produto)
- [ ] Read models (CQRS) para consultas otimizadas
- **Critério:** relatório atualizado em tempo quase real a partir do stream.

## 🔀 Fase 13 — Variação coreografada
- [ ] Implementar a mesma saga em **coreografia** e documentar o comparativo
- **Critério:** ADR comparando orquestração x coreografia com trade-offs medidos.

## ☸️ Fase 14 — Kubernetes
- [ ] Helm/Kustomize em k3d/minikube; HPA; readiness/liveness; ConfigMaps/Secrets
- [ ] Discovery nativo do cluster no lugar do Eureka
- **Critério:** `helm install` sobe o sistema; escala sob carga.

## 🛡️ Fase 15 — Resiliência avançada
- [ ] Rate limiting distribuído (Redis), bulkhead, retry budgets, fallbacks
- **Critério:** degradação graciosa sob falha/sobrecarga de um serviço.

## 🤝 Fase 16 — Contract testing
- [ ] Spring Cloud Contract ou Pact entre produtores/consumidores
- **Critério:** quebra de contrato falha o build do produtor.

## 🚦 Fase 17 — Deploy progressivo
- [ ] Blue-green / canary; GitOps (ArgoCD)
- **Critério:** rollout/rollback sem downtime.

## 🔭 Fase 18 — Observabilidade avançada
- [ ] OpenTelemetry Collector + stack LGTM (Loki/Grafana/Tempo)
- **Critério:** logs, métricas e traces correlacionados em um só lugar.

## 📚 Fase 19 — Documentação completa
- [ ] Adotar a pasta `doc/` (ARCHITECTURE/DEVELOPMENT/TESTS/DEPLOYMENT) + ADRs numerados,
      no padrão dos projetos `partners-api`/`picpay-simplificado`
- **Critério:** documentação navegável com índice e ADRs versionados.
