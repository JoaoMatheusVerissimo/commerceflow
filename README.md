# CommerceFlow

CommerceFlow é um projeto de portfólio que evoluirá para uma plataforma de comércio digital orientada a eventos, reunindo e-commerce, CRM, Customer 360 e analytics.

> Status atual: **Fase 3 — Cart implementada e validada localmente; aceite remoto aguarda CI.** Carrinho persistente, favoritos, cupons e revisão inicial do checkout estão disponíveis. Ainda não há pedidos, estoque ou pagamentos.

## O que existe agora

- React/TypeScript com loja, busca/filtros, paginação, detalhes e seleção de variantes, cadastro/login e conta;
- carrinho autenticado, favoritos, cotação autoritativa, cupom e revisão inicial do checkout;
- gestão de produtos/categorias/cupons por MANAGER/ADMIN e moderação de avaliações;
- gateway e serviços Auth/Customer/Catalog/Order em Java 21, bancos PostgreSQL separados e Flyway;
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

- [Briefing completo](docs/PROJECT-BRIEF.md)
- [Índice da arquitetura](docs/architecture/README.md)
- [Catálogo das APIs](docs/api/api-catalog.md)
- [Catálogo de eventos](docs/events/event-catalog.md)
- [Architecture Decision Records](docs/decisions/README.md)
- [Roadmap técnico](docs/roadmap.md)

## Executar localmente

Configure uma senha local em `.env` a partir de `.env.example` e execute `docker compose up -d --build`. Abra `http://localhost:3000`. Requer Docker Engine/Desktop com Compose v2 e containers Linux. Consulte [o guia da Fase 3](docs/cart.md) para testes, variáveis, APIs e execução.

## Validação e limitações

Na Fase 3, 44 testes Java e 22 frontend passaram, assim como Checkstyle, ESLint, builds, auditoria npm e os smokes Foundation/Catalog/Cart no Compose local. O aceite remoto desta alteração depende da próxima CI após o commit/push do usuário. A última CI publicada antes dela continua sendo a [Fase 2 no commit 528de2a](https://github.com/JoaoMatheusVerissimo/commerceflow/actions/runs/34667831031). Não há deploy público nem configuração pronta para produção. Veja [evidências e limites](docs/cart.md#verificação) e [ADR-012](docs/decisions/ADR-012-cart-and-pricing.md).

Os aceites anteriores estão em [Foundation](docs/foundation.md#aceite-da-fase-1) e [Catalog](docs/catalog.md). A Fase 3 está documentada em [Cart](docs/cart.md). A Fase 4 não foi iniciada e exige nova aprovação explícita.

## Princípios

- limites de domínio antes de tecnologia;
- consistência local forte e consistência distribuída explícita;
- contratos versionados para HTTP e eventos;
- segurança, privacidade e observabilidade desde a fundação;
- funcionalidades reais e testadas, sem simulações apresentadas como produto pronto.

O escopo completo, a Definition of Done e as restrições do projeto estão em [docs/PROJECT-BRIEF.md](docs/PROJECT-BRIEF.md).
