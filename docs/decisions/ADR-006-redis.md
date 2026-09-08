# ADR-006 — Redis com uso restrito e mensurável

- Status: Aceito
- Data: 2026-09-08

## Contexto

Catálogo possui leituras frequentes e o gateway precisa limitar abuso. Redis é útil, mas seu uso indiscriminado cria duas fontes de verdade e invalidação difícil.

## Decisão

Usar Redis inicialmente para cache-aside de consultas públicas de catálogo e contadores de rate limiting. PostgreSQL permanece fonte de verdade. Escritas de catálogo invalidam chaves relacionadas por versão/namespace; TTL limita staleness.

Sessões de refresh permanecem em PostgreSQL no início. Carrinho também permanece persistido pelo Order Service, sem depender de Redis para durabilidade.

## Consequências

- queda do Redis degrada cache/rate limit de forma controlada, sem perder dados de negócio;
- métricas de hit rate, latência e evictions justificam permanência;
- invalidação e limites de staleness precisam de testes;
- novos usos exigem problema, fallback e ownership documentados.

## Alternativas consideradas

- Redis como banco primário de carrinho/sessão: adiado por requisitos de durabilidade e simplicidade.
- Nenhum cache: válido inicialmente; Redis entra quando a fase correspondente medir benefício.
