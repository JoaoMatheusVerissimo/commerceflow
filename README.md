# CommerceFlow

CommerceFlow é um projeto de portfólio que evoluirá para uma plataforma de comércio digital orientada a eventos, reunindo e-commerce, CRM, Customer 360 e analytics.

> Status atual: **Fase 4 — Orders implementada e validada localmente; aceite remoto aguarda CI.** Pedidos com snapshot, reserva concorrente de estoque, histórico e visão operacional estão disponíveis. Pagamentos ainda não foram iniciados.

## O que existe agora

- React/TypeScript com loja, busca/filtros, paginação, detalhes e seleção de variantes, cadastro/login e conta;
- carrinho autenticado, favoritos, cotação autoritativa e cupons;
- checkout idempotente, pedidos com snapshot, histórico/detalhe e reserva de estoque segura;
- painel inicial de pedidos e estoque com movimentos auditáveis e alerta de mínimo;
- gestão de produtos/categorias/cupons por MANAGER/ADMIN e moderação de avaliações;
- gateway e serviços Auth/Customer/Catalog/Order/Inventory em Java 21, bancos PostgreSQL separados e Flyway;
- JWT RS256, Argon2, refresh rotativo, CSRF, roles e erros padronizados;
- seed fictício com seis produtos e imagens locais, Dockerfiles, Compose, testes e CI;

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
- [Executar, administrar e testar o catálogo](docs/catalog.md)
- [Executar e testar carrinho, favoritos, cupons e checkout inicial](docs/cart.md)
- [Executar e testar pedidos e estoque](docs/orders.md)

- [Briefing completo](docs/PROJECT-BRIEF.md)
- [Índice da arquitetura](docs/architecture/README.md)
- [Catálogo das APIs](docs/api/api-catalog.md)
- [Catálogo de eventos](docs/events/event-catalog.md)
- [Architecture Decision Records](docs/decisions/README.md)
- [Roadmap técnico](docs/roadmap.md)

## Executar localmente

Configure uma senha local em `.env` a partir de `.env.example` e execute `docker compose up -d --build`. Abra `http://localhost:3000`. Requer Docker Engine/Desktop com Compose v2 e containers Linux. Consulte [o guia da Fase 4](docs/orders.md) para testes, variáveis, APIs e execução.

## Validação e limitações

Na Fase 4, 60 testes Java e 25 frontend passaram, assim como Checkstyle, ESLint, builds, auditoria npm e os quatro smokes no Compose local. O aceite remoto desta alteração depende da próxima CI após o commit/push do usuário. A Fase 3 já possui [CI verde no commit e3c8d22](https://github.com/JoaoMatheusVerissimo/commerceflow/actions/runs/35297557804). Não há deploy público nem configuração pronta para produção. Veja [evidências e limites](docs/orders.md#verificação) e [ADR-013](docs/decisions/ADR-013-orders-and-inventory.md).

Os aceites anteriores estão em [Foundation](docs/foundation.md#aceite-da-fase-1), [Catalog](docs/catalog.md) e [Cart](docs/cart.md). A Fase 4 está documentada em [Orders](docs/orders.md). A Fase 5 não foi iniciada e exige nova aprovação explícita.

## Princípios

- limites de domínio antes de tecnologia;
- consistência local forte e consistência distribuída explícita;
- contratos versionados para HTTP e eventos;
- segurança, privacidade e observabilidade desde a fundação;
- funcionalidades reais e testadas, sem simulações apresentadas como produto pronto.

O escopo completo, a Definition of Done e as restrições do projeto estão em [docs/PROJECT-BRIEF.md](docs/PROJECT-BRIEF.md).
