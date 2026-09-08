# ADR-003 — Limites por capacidade de negócio

- Status: Aceito
- Data: 2026-09-08

## Contexto

O briefing deseja microsserviços, mas alerta contra serviços criados apenas por aparência. Limites errados geram comunicação excessiva, transações distribuídas e modelos compartilhados.

## Decisão

Adotar os contextos Auth, Customer, Catalog, Inventory, Order, Payment, CRM, Notification e Analytics, além do Gateway sem domínio. Cada um possui autoridade de escrita e dados próprios, conforme [services.md](../architecture/services.md).

Carrinho permanece em Order; cupons/preços em Catalog; score avançado é calculado em Analytics e materializado operacionalmente em Customer. Os serviços serão implementados progressivamente, não todos na fundação.

## Consequências

- ownership e vocabulário ficam explícitos;
- algumas telas dependem de projeções e consistência eventual;
- duplicação controlada de snapshots é intencional;
- uma extração ou união futura exige evidência e novo ADR;
- não haverá biblioteca compartilhada de entidades de domínio.

## Alternativas consideradas

- Microsserviço por entidade: rejeitado por granularidade e acoplamento excessivos.
- Modular monolith permanente: reduziria operação, mas não exercitaria os requisitos distribuídos centrais do portfólio.
