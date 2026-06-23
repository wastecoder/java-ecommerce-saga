# ADR-0005: Consumidores idempotentes + Dead Letter Topic

## Status

Aceito.

## Contexto

O Kafka entrega mensagens **at-least-once**: um rebalanceamento de partição, um retry ou um *commit*
de offset perdido podem reentregar a **mesma** mensagem. Como os consumidores têm efeitos colaterais
(reservar estoque, cobrar pagamento, mudar o estado do pedido), **reprocessar não pode duplicar o
efeito**. Além disso, uma mensagem "venenosa" (que sempre falha) não pode travar a partição
indefinidamente.

## Decisão

- **Idempotência:** cada consumidor grava o `eventId` já processado em uma tabela
  `processed_messages` (`eventId`, `consumer`, `processedAt`) e **ignora duplicatas** antes de aplicar
  o efeito.
- **Dead Letter Topic:** cada consumidor tem um `<topic>.DLT`; após os *retries*, a mensagem é
  publicada no DLT via `DeadLetterPublishingRecoverer` do Spring Kafka, em vez de bloquear o consumo.
- **Ordenação e não-duplicação na origem:** produtor **idempotente** e **partição por `orderId`**
  garantem ordem por pedido.

## Alternativas consideradas

- **Confiar só no *exactly-once*/transações do Kafka.** Não adotada no MVP: não cobre os efeitos
  colaterais fora do broker (escrita no banco, chamada ao PSP) e é mais complexo. O **Transactional
  Outbox** (atomicidade real DB+broker) fica registrado como evolução futura (`FUTURE-PROGRESS.md`).

## Consequências

**Positivas:**
- Reprocessamento é **seguro**; a mesma mensagem não altera o resultado duas vezes.
- Mensagens não-processáveis ficam isoladas no DLT, sem travar a partição.

**Negativas / custos:**
- Uma tabela e uma checagem extra por mensagem em cada consumidor.
- Ainda **não** há atomicidade real entre escrever no banco e publicar no broker (sem Outbox): existe
  uma janela pequena entre aplicar o efeito e marcar o `eventId` como processado.
