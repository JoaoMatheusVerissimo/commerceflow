# ADR-008 — Contratos explícitos e versionados

- Status: Aceito
- Data: 2026-09-08

## Contexto

Deploys independentes e consumers assíncronos tornam mudanças incompatíveis especialmente perigosas. Documentação manual sem validação tende a divergir do código.

## Decisão

Cada serviço publica OpenAPI para HTTP e schemas versionados para eventos. O monorepo mantém contratos de integração em `contracts`, com compatibilidade validada na CI. Payloads evoluem de forma aditiva por padrão; breaking changes criam nova major version e período de convivência.

O formato de erro HTTP e o envelope de eventos são contratos transversais, sem compartilhar modelos internos.

## Consequências

- mudanças de contrato tornam-se visíveis em review;
- testes de consumidor/produtor e lint entram na CI;
- versões antigas precisam de política de depreciação;
- geração de clientes é permitida, mas o contrato não dita o modelo de domínio;
- specs específicas só são adicionadas junto de endpoints/eventos reais.

## Alternativas consideradas

- DTOs Java compartilhados: rejeitados por acoplamento de linguagem e deploy.
- Documentação apenas em README: rejeitada por não ser validável.
