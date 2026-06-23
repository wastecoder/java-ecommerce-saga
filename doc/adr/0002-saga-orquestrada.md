# ADR-0002: SAGA orquestrada (orchestration), não coreografia

## Status

Aceito.

## Contexto

Criar um pedido exige **reservar estoque**, **cobrar o pagamento** e **confirmar (ou desfazer)** a
operação de forma consistente entre serviços independentes, **sem** transação distribuída tradicional
(2PC). Esse é o problema clássico que a **SAGA** resolve, e é o coração deste projeto. Falta decidir
como coordenar os passos e as **compensações**.

## Decisão

Usar uma **SAGA orquestrada**: o `order-service` é o **orquestrador**. Ele emite comandos
(`inventory.commands` → `ReserveStock`/`ReleaseStock`, `payment.commands` → `ProcessPayment`) e **reage
às respostas** (`inventory.events`, `payment.events`), avançando a saga ou **compensando**
(`ReleaseStock` quando o pagamento falha). O estado da saga vive no próprio agregado `Order`
(`OrderStatus`), e cada transição publica um evento em `order.events`.

Detalhes do fluxo e da máquina de estados em [ARCHITECTURE.md](../ARCHITECTURE.md).

## Alternativas consideradas

- **Coreografia** (cada serviço reage a eventos dos outros, sem coordenador central). Não adotada: a
  lógica de transição e, principalmente, de **compensação** fica espalhada por vários serviços —
  difícil de seguir, debugar e explicar. A variação coreografada fica registrada como evolução futura
  (ver `FUTURE-PROGRESS.md`).

## Consequências

**Positivas:**
- Fluxo central **explícito**: um único lugar concentra a sequência e as compensações.
- Mais fácil de **rastrear** (o trace ponta-a-ponta da saga aparece como um único trace no Zipkin) e de
  explicar.

**Negativas / custos:**
- O orquestrador concentra o acoplamento lógico do fluxo.
- É um ponto único de coordenação — se a lógica do orquestrador cresce, ele precisa ser bem fatiado
  (daí o `OrderSagaCoordinator` separado dos casos de uso de entrada).
