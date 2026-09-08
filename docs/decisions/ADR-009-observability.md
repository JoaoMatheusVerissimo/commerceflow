# ADR-009 — OpenTelemetry e correlação ponta a ponta

- Status: Aceito
- Data: 2026-09-08

## Contexto

Uma jornada atravessa navegador, gateway, serviços e Kafka. Logs isolados não explicam latência, retries, duplicatas ou compensações.

## Decisão

Padronizar OpenTelemetry para traces, métricas e contexto; W3C Trace Context em HTTP/Kafka; `correlationId` no protocolo de negócio; logs estruturados com IDs de trace/span. Prometheus e Grafana compõem a visualização inicial.

Instrumentação mínima nasce com cada serviço. A stack completa executa em profile separado na Fase 13.

## Consequências

- propagação de contexto precisa de testes de integração;
- sampling e retenção serão configurados por ambiente;
- PII e payload bruto são proibidos em telemetry;
- labels de métricas devem evitar alta cardinalidade;
- alertas serão calibrados com comportamento real, sem SLOs fictícios.

## Alternativas consideradas

- Biblioteca proprietária de observabilidade: rejeitada por lock-in e manutenção.
- Apenas logs: rejeitado por baixa capacidade de reconstruir fluxos distribuídos.
