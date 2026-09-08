# ADR-004 — Transactional Outbox e Inbox idempotente

- Status: Aceito
- Data: 2026-09-08

## Contexto

Persistir uma mudança e publicar no Kafka em operações separadas cria janelas em que apenas uma delas ocorre. Transação distribuída 2PC aumentaria complexidade e acoplamento.

## Decisão

Serviços produtores gravam o agregado e a Outbox na mesma transação PostgreSQL. Um relay publica mensagens pendentes. Consumidores registram `eventId` em Inbox/processed-events na mesma transação de seus efeitos.

A publicação pode se repetir; a correção vem da idempotência, não de alegar exactly-once ponta a ponta.

## Consequências

- nenhuma mudança confirmada perde sua intenção de publicação;
- relay, limpeza, métricas e alertas da Outbox precisam existir;
- handlers devem possuir chaves de negócio idempotentes além do `eventId`;
- DLQ e replay tornam-se operações explícitas e auditadas;
- o padrão será criado como infraestrutura local de cada serviço, evitando um pacote de domínio compartilhado.

## Alternativas consideradas

- Publicar depois do commit: rejeitado pela janela de perda.
- Publicar antes do commit: rejeitado pelo risco de evento sobre transação revertida.
- 2PC: rejeitado por disponibilidade e suporte operacional.
