# ADR-0003: Kafka via Spring Kafka direto, em modo KRaft

## Status

Aceito.

## Contexto

A comunicação entre serviços é **assíncrona e event-driven**, e um dos objetivos de aprendizado
explícitos do projeto é **Apache Kafka** (produtores/consumidores, idempotência, DLQ, ordenação). A
escolha do cliente e do modo de operação do broker afeta o quanto desses conceitos fica visível.

## Decisão

Usar **Apache Kafka** em modo **KRaft** (sem Zookeeper), com o cliente **Spring Kafka** **direto**
(`@KafkaListener`, `KafkaTemplate`) — não Spring Cloud Stream. **Produtor idempotente** ligado e
**chave de partição = `orderId`** (ordenação por pedido). Nomes de tópicos e tipos de mensagem ficam
centralizados em constantes no adapter de mensageria.

## Alternativas consideradas

- **Spring Cloud Stream.** Não adotada: abstrai o broker atrás de bindings e esconde justamente os
  conceitos de Kafka que o projeto quer praticar.
- **RabbitMQ.** Não adotada: modelo de fila/broker diferente do log distribuído; o domínio do projeto
  (eventos, replay, ordenação por chave) combina melhor com o modelo de log do Kafka.

## Consequências

**Positivas:**
- Contato direto com a API do Spring Kafka (listeners, error handlers, `KafkaTemplate`).
- KRaft dispensa o Zookeeper — menos um componente de infraestrutura no Compose.

**Negativas / custos:**
- Mais código de configuração explícita (produtor, consumidores, error handler / DLT) do que a
  abstração do Stream ofereceria.
