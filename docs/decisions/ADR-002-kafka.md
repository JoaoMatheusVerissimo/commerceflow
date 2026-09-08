# ADR-002 — Kafka para integração assíncrona

- Status: Aceito
- Data: 2026-09-08

## Contexto

Checkout, automações, Customer 360, CRM, notificações e analytics precisam reagir aos mesmos fatos, sobreviver a indisponibilidade temporária e permitir replay controlado.

## Decisão

Usar Apache Kafka para eventos de domínio e comandos assíncronos. Mensagens possuem envelope versionado, chave por agregado, trace context e contrato de payload. A entrega assumida é pelo menos uma vez.

HTTP continua sendo usado para consultas e validações imediatas; Kafka não substitui todo tráfego síncrono.

## Consequências

- produtores não ficam acoplados a todos os consumidores;
- consumers precisam de idempotência, monitoramento de lag, retry e DLQ;
- ordenação só é garantida dentro da chave/partição;
- contratos e evolução passam a ser parte da Definition of Done;
- operação local fica mais pesada e será introduzida apenas na Fase 6.

## Alternativas consideradas

- Chamadas HTTP encadeadas: simples no início, mas frágeis para fan-out e indisponibilidade.
- RabbitMQ: adequado a filas, porém Kafka se alinha melhor a histórico de eventos, replay e analytics do projeto.
