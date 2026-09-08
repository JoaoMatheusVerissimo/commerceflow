# Architecture Decision Records

ADRs registram decisões com impacto duradouro. Um ADR aceito não é reescrito para esconder evolução; uma nova decisão o substitui explicitamente.

| ADR | Status | Decisão |
|---|---|---|
| [ADR-001](ADR-001-postgresql.md) | Aceito | PostgreSQL como banco transacional principal |
| [ADR-002](ADR-002-kafka.md) | Aceito | Kafka para integração assíncrona orientada a eventos |
| [ADR-003](ADR-003-service-boundaries.md) | Aceito | Limites de microsserviços por capacidade de negócio |
| [ADR-004](ADR-004-transactional-outbox.md) | Aceito | Transactional Outbox e Inbox idempotente |
| [ADR-005](ADR-005-orchestrated-saga.md) | Aceito | Saga de checkout orquestrada pelo Order Service |
| [ADR-006](ADR-006-redis.md) | Aceito | Redis restrito a cache e rate limiting justificados |
| [ADR-007](ADR-007-authentication.md) | Aceito | JWT assimétrico curto e refresh token rotativo |
| [ADR-008](ADR-008-contract-versioning.md) | Aceito | Contratos HTTP/evento explícitos e versionados |
| [ADR-009](ADR-009-observability.md) | Aceito | OpenTelemetry e correlação de ponta a ponta |

## Estados

- **Proposto:** em análise.
- **Aceito:** baseline para implementação.
- **Substituído:** preservado, mas apontando para o ADR sucessor.
- **Rejeitado:** analisado e não adotado.
