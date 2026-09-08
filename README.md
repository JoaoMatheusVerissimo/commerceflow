# CommerceFlow

CommerceFlow é um projeto de portfólio que evoluirá para uma plataforma de comércio digital orientada a eventos, reunindo e-commerce, CRM, Customer 360 e analytics.

> Status atual: **Fase 0 — Arquitetura concluída em documentação.** Não há funcionalidades de aplicação implementadas ainda.

## O que existe agora

- visão geral e diagramas da arquitetura;
- limites e responsabilidades dos serviços;
- modelo de domínio e estratégia de persistência;
- catálogo inicial de APIs e eventos;
- fluxo de checkout e Saga com compensação;
- estratégias de segurança e observabilidade;
- ADRs das decisões estruturais;
- roadmap técnico por incrementos demonstráveis.

## Documentação

- [Briefing completo](docs/PROJECT-BRIEF.md)
- [Índice da arquitetura](docs/architecture/README.md)
- [Catálogo das APIs](docs/api/api-catalog.md)
- [Catálogo de eventos](docs/events/event-catalog.md)
- [Architecture Decision Records](docs/decisions/README.md)
- [Roadmap técnico](docs/roadmap.md)

## Próximo passo

A Fase 1 criará a fundação executável: monorepo, infraestrutura local mínima, gateway, autenticação, frontend e CI inicial. Ela só começa após aprovação explícita da Fase 0.

## Princípios

- limites de domínio antes de tecnologia;
- consistência local forte e consistência distribuída explícita;
- contratos versionados para HTTP e eventos;
- segurança, privacidade e observabilidade desde a fundação;
- funcionalidades reais e testadas, sem simulações apresentadas como produto pronto.

O escopo completo, a Definition of Done e as restrições do projeto estão em [docs/PROJECT-BRIEF.md](docs/PROJECT-BRIEF.md).
