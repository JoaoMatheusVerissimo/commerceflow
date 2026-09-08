# Estratégia de observabilidade

## Objetivo

Permitir seguir uma jornada da interface ao gateway, serviços, Kafka e compensações sem depender de logs manuais desconectados.

## Stack alvo

- OpenTelemetry SDK/auto-instrumentation em React, Java e Python;
- OpenTelemetry Collector como ponto de recepção e roteamento;
- Prometheus para métricas;
- Grafana para dashboards e correlação;
- exportador/backend de traces compatível com OpenTelemetry no profile de observabilidade;
- logs JSON em stdout, com backend centralizado adicionado apenas quando houver necessidade operacional comprovada.

Infraestrutura de observabilidade roda em profile Docker separado para não tornar o fluxo básico excessivamente pesado.

## Contexto distribuído

- HTTP usa W3C Trace Context (`traceparent` e `tracestate`).
- Gateway aceita um `X-Correlation-Id` UUID válido ou cria outro; sempre o devolve.
- Kafka propaga trace context em headers e `correlationId` no envelope.
- Consumers criam spans do tipo consumer ligados ao contexto recebido.
- Retries e compensações preservam `correlationId`, mas criam spans próprios.

`correlationId` pode durar por toda a Saga. `traceId` descreve uma execução técnica e pode mudar após processamento diferido.

## Logs estruturados

Campos mínimos: `timestamp`, `level`, `service`, `environment`, `message`, `traceId`, `spanId`, `correlationId`, `eventType`, `aggregateId` quando aplicável e `errorCode`.

Logs não contêm senha, token, cookie, authorization header, endereço, telefone, e-mail completo ou payload bruto. Stack traces ficam restritos a erro interno e são sanitizados na resposta pública.

## Métricas

### Técnicas

- taxa, erro e duração HTTP por rota normalizada;
- uso do pool de conexões e duração de query;
- JVM/Python process metrics;
- publish/consume rate, lag, retries e DLQ por consumer group;
- idade e tamanho da Outbox;
- cache hit/miss e rate-limit rejections.

### Negócio/fluxo

- checkouts iniciados, confirmados e cancelados;
- duração da Saga por etapa;
- taxa de falha de reserva e pagamento;
- quantidade e idade de Sagas paradas;
- compensações e reembolsos;
- carrinhos abandonados;
- low-stock alerts;
- mudanças de segmento e churn de alta confiança.

Métricas não usam `customerId`, `orderId` ou SKU como label para evitar alta cardinalidade. Esses valores pertencem a logs/traces.

## Health checks

- liveness verifica apenas se o processo pode responder;
- readiness verifica dependências necessárias para receber tráfego;
- indisponibilidade de uma integração opcional não mata o processo;
- health endpoints não expõem credenciais, hosts internos ou stack traces.

## Dashboards e alertas iniciais

1. visão RED por serviço;
2. Kafka lag, DLQ e Outbox atrasada;
3. funil do checkout e duração da Saga;
4. PostgreSQL e pools;
5. erros de autenticação e rate limits.

Alertas priorizam sintomas acionáveis: Sagas paradas, DLQ crescente, erro sustentado, latência degradada e falta de publicação da Outbox. Limiares serão calibrados com dados reais; não serão apresentados SLOs fictícios.

## Evolução

A instrumentação mínima (correlation ID, logs estruturados e health) nasce com cada serviço. Collector, Prometheus, Grafana e tracing distribuído completo entram na Fase 13, com testes de propagação e runbooks.
