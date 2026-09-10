# CommerceFlow

CommerceFlow é um projeto de portfólio que evoluirá para uma plataforma de comércio digital orientada a eventos, reunindo e-commerce, CRM, Customer 360 e analytics.

> Status atual: **Fase 1 — Foundation implementada, com aceite Docker/CI pendente.** Cadastro, login e perfil autenticado disponíveis; ainda não há catálogo ou compras.

## O que existe agora

- React/TypeScript com cadastro, login e página de conta;
- gateway e serviços Auth/Customer em Java 21, bancos PostgreSQL separados e Flyway;
- JWT RS256, Argon2, refresh rotativo, CSRF, roles e erros padronizados;
- Dockerfiles, Compose, testes automatizados e workflow de CI inicial;

- visão geral e diagramas da arquitetura;
- limites e responsabilidades dos serviços;
- modelo de domínio e estratégia de persistência;
- catálogo inicial de APIs e eventos;
- fluxo de checkout e Saga com compensação;
- estratégias de segurança e observabilidade;
- ADRs das decisões estruturais;
- roadmap técnico por incrementos demonstráveis.

## Documentação

- [Executar e testar a Foundation](docs/foundation.md)

- [Briefing completo](docs/PROJECT-BRIEF.md)
- [Índice da arquitetura](docs/architecture/README.md)
- [Catálogo das APIs](docs/api/api-catalog.md)
- [Catálogo de eventos](docs/events/event-catalog.md)
- [Architecture Decision Records](docs/decisions/README.md)
- [Roadmap técnico](docs/roadmap.md)

## Executar localmente

Configure uma senha local em `.env` a partir de `.env.example` e execute `docker compose up -d --build`. Abra `http://localhost:3000`. Requer Docker Engine/Desktop com Compose v2 e containers Linux. Consulte [o guia completo](docs/foundation.md) para testes, variáveis, OpenAPI e execução Java/Vite.

## Validação e limitações

Build/lint Java e frontend e testes locais executados. Na [CI do commit b494875](https://github.com/JoaoMatheusVerissimo/commerceflow/actions/runs/34429464238), Java passou, frontend falhou nos testes e Compose foi pulado. A correção de isolamento dos testes React passou localmente; falta enviá-la e validar novamente a CI e o smoke Compose. Docker não está disponível neste computador. Não há deploy público. Esta é uma base demonstrativa, não uma configuração pronta para produção. Veja [ADR-010](docs/decisions/ADR-010-foundation-integration.md).

A próxima ação é validar Compose/CI para fechar o aceite da Fase 1. A Fase 2 não foi iniciada e exige nova aprovação explícita.

## Princípios

- limites de domínio antes de tecnologia;
- consistência local forte e consistência distribuída explícita;
- contratos versionados para HTTP e eventos;
- segurança, privacidade e observabilidade desde a fundação;
- funcionalidades reais e testadas, sem simulações apresentadas como produto pronto.

O escopo completo, a Definition of Done e as restrições do projeto estão em [docs/PROJECT-BRIEF.md](docs/PROJECT-BRIEF.md).
