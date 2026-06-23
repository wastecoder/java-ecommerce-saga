# ADR-0006: Tratamento de erros com Problem Details (RFC 7807)

## Status

Aceito.

## Contexto

Os serviços expõem APIs REST e precisam de um **contrato de erro consistente** — entre endpoints e
entre serviços. Se cada controller monta o seu próprio formato de erro, ou se as exceções carregam o
status HTTP, a regra de negócio acaba **acoplada ao HTTP** e o cliente recebe respostas heterogêneas.

## Decisão

Padronizar todas as respostas de erro em **Problem Details (RFC 7807)**. As exceções de domínio são
**HTTP-agnósticas** (não conhecem status code); quem traduz exceção → resposta HTTP é **exclusivamente**
um `GlobalExceptionHandler` (`@RestControllerAdvice`) por serviço, apoiado em um enum **`ProblemType`**
(slug, título, status). O `type` da resposta é montado a partir de uma base URI configurável
(`https://wastecoder.com/problems`) + o slug do `ProblemType`.

A taxonomia de tipos de problema está em [API.md](../API.md).

## Alternativas consideradas

- **Erros ad-hoc por controller.** Rejeitada: formato inconsistente e duplicação da lógica de
  tradução.
- **Exceções carregando o status HTTP.** Rejeitada: acopla o domínio ao transporte HTTP.

## Consequências

**Positivas:**
- Contrato de erro **uniforme** (mesmo formato em todos os serviços), fácil de consumir.
- Domínio limpo: exceções expressam o problema de negócio, não o protocolo.

**Negativas / custos:**
- Um `GlobalExceptionHandler` + `ProblemType` para manter em cada serviço.
