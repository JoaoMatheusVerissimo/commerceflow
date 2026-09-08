# Arquitetura — Fase 0

Este diretório reúne a arquitetura inicial do CommerceFlow. As decisões foram feitas para permitir evolução incremental sem esconder os desafios de consistência distribuída.

## Entregáveis

| # | Entregável | Documento |
|---|---|---|
| 1 | Arquitetura geral | [system-overview.md](system-overview.md) |
| 2 | Diagramas | [system-overview.md](system-overview.md) e [checkout-and-saga.md](checkout-and-saga.md) |
| 3 | Limites dos microsserviços | [services.md](services.md) |
| 4 | Estrutura do monorepo | [monorepo.md](monorepo.md) |
| 5 | Entidades principais | [domain-model.md](domain-model.md) |
| 6 | Bancos de dados | [database.md](database.md) |
| 7 | Eventos Kafka | [../events/event-catalog.md](../events/event-catalog.md) |
| 8 | APIs principais | [../api/api-catalog.md](../api/api-catalog.md) |
| 9 | Fluxo completo de checkout | [checkout-and-saga.md](checkout-and-saga.md) |
| 10 | Fluxo Saga | [checkout-and-saga.md](checkout-and-saga.md) |
| 11 | Estratégia de autenticação | [security.md](security.md) |
| 12 | Estratégia de observabilidade | [observability.md](observability.md) |
| 13 | ADRs iniciais | [../decisions/README.md](../decisions/README.md) |
| 14 | Roadmap técnico | [../roadmap.md](../roadmap.md) |

## Estado da decisão

Esta é a baseline arquitetural. Mudanças relevantes devem ser registradas em ADR antes da implementação. A aprovação desta fase autoriza apenas o início da Fase 1; não torna funcionalidades posteriores parte do produto atual.
