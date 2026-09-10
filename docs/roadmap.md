# Roadmap técnico

O roadmap entrega fatias verificáveis e mantém a ordem visual exigida: loja funcional, painel administrativo, CRM Kanban, Customer 360, Analytics e observabilidade.

## Gates gerais

Uma fase só termina quando a funcionalidade executa localmente, possui validação e tratamento de erro, cobre regras críticas com testes, atualiza contratos/documentação, não expõe secrets e mantém CI verde. Não há avanço automático entre fases.

## Fase 0 — Arquitetura

**Resultado:** baseline de arquitetura, limites, dados, contratos, Saga, segurança, observabilidade, ADRs e estrutura progressiva do monorepo.

**Gate:** revisão/aprovação explícita antes de criar a fundação executável.

## Fase 1 — Foundation

**Status:** concluída. Aceite registrado após sucesso dos jobs Java, frontend e Compose na [execução Foundation #2](https://github.com/JoaoMatheusVerissimo/commerceflow/actions/runs/34430111496), commit `48e5237`. Veja [evidências e limitações](foundation.md#aceite-da-fase-1). Fase 2 não iniciada; exige nova aprovação explícita.

**Fatia demonstrável:** cadastro, login e acesso autenticado a uma página protegida.

- workspace do frontend e builds Java;
- PostgreSQL e infraestrutura Docker mínima;
- Gateway, Auth Service e primeira capacidade do Customer Service necessária ao registro;
- JWT/refresh rotativo, roles e tratamento padrão de erro;
- React com layout inicial da loja e estados loading/error/success;
- CI de lint, build e testes; OpenAPI dos endpoints reais.

## Fase 2 — Catalog

**Fatia demonstrável:** navegar, buscar, filtrar e abrir um produto real do seed.

- Catalog Service, categorias, variantes, imagens e avaliações iniciais;
- administração mínima de catálogo necessária para manter os dados;
- paginação, índices e cache somente após baseline medida;
- testes de preço, slug/SKU, busca e permissões.

## Fase 3 — Cart

**Fatia demonstrável:** adicionar/remover itens, favoritar, aplicar cupom e revisar checkout.

- agregado Cart no Order Service;
- cotação autoritativa no Catalog;
- favoritos no Customer;
- persistência, idempotência de comandos e experiência responsiva.

## Fase 4 — Orders

**Fatia demonstrável:** criar pedido, reservar estoque com segurança concorrente e acompanhar status inicial.

- Order e Inventory Services;
- snapshots, migrations, estoque/movimentações/reservas;
- teste concorrente obrigatório para última unidade;
- páginas de histórico/detalhe e painel operacional inicial.

## Fase 5 — Payments

**Fatia demonstrável:** aprovação, recusa, timeout e compensação sem Kafka.

- Payment Service simulado, sem dinheiro/dados reais;
- máquina de estado e Saga inicialmente adaptada à integração disponível;
- idempotência, reconciliação e liberação de estoque;
- testes completos de caminhos feliz e de falha.

## Fase 6 — Kafka

**Fatia demonstrável:** checkout executa a Saga assíncrona e tolera duplicatas/indisponibilidade.

- Kafka, tópicos, schemas reais e trace headers;
- Outbox/Inbox, retries, DLQ e replay auditado;
- migração controlada das integrações apropriadas;
- testes de duplicata, indisponibilidade e retomada.

## Fase 7 — CRM

**Fatia demonstrável:** equipe move oportunidades reais no Kanban e registra trabalho comercial.

- CRM Service, pipelines, leads, oportunidades, tarefas, notas e interações;
- drag-and-drop com controle de versão e rollback visual;
- autorização por role/ownership e audit trail.

## Fase 8 — Customer 360

**Fatia demonstrável:** perfil apresenta pedidos, métricas, timeline e atividade consolidados.

- projeções event-driven no Customer Service;
- labels, métricas operacionais e paginação de timeline;
- tratamento de atraso/replay e reconciliação.

## Fase 9 — Automações

**Fatia demonstrável:** regras versionadas criam labels, alertas e oportunidades sem duplicação.

- terceira compra/recorrente, gasto/VIP, abandono, 90 dias/risco e low stock;
- condições e ações explicáveis, idempotentes e auditáveis;
- scheduler seguro para regras temporais.

## Fase 10 — Analytics Python

**Fatia demonstrável:** dashboard e Customer 360 exibem RFM, segmentos e recomendações explicáveis.

- FastAPI, Pandas quando útil, migrations e pytest;
- feature snapshots, RFM e recomendação comportamental;
- publicação/materialização de resultados versionados;
- avaliação de qualidade dos dados e limitações.

## Fase 11 — Churn

**Fatia demonstrável:** risco experimental apresenta percentual, classe e motivos reproduzíveis.

- baseline determinística antes de ML;
- dataset, features, validação temporal, métricas e limitações documentadas;
- nenhuma alegação de precisão real baseada somente no seed.

## Fase 12 — Notifications

**Fatia demonstrável:** eventos geram notificações internas e e-mail simulado; UI recebe atualização em tempo real.

- Notification Service, preferências, templates e tentativas;
- WebSocket autenticado, retry e deduplicação;
- opt-out e categorias transacionais versus comerciais.

## Fase 13 — Observability

**Fatia demonstrável:** um trace mostra checkout completo e compensação através dos serviços/Kafka.

- Collector, Prometheus, Grafana e backend de tracing;
- dashboards RED, Kafka/Outbox e Saga;
- alertas, sampling, redaction e runbooks;
- testes de propagação do contexto.

## Fase 14 — Quality

**Fatia demonstrável:** suíte e evidências cobrem carga, segurança, resiliência e concorrência prioritárias.

- revisão de queries, índices, N+1, payload e cache;
- testes de falha, permissões, replay e recuperação;
- análise de dependências, imagens e configuração;
- performance baseada em metas e medições documentadas.

## Fase 15 — Portfolio

**Resultado:** demonstração reproduzível e documentação fiel ao que existe.

- seed exclusivamente fictício e contas demo seguras;
- README final, screenshots, GIFs, diagramas e catálogo de APIs;
- fluxo feliz e de compensação demonstráveis;
- CI/CD, instruções locais, decisões e limitações revisadas.

## Estratégia de branches e commits

Branches seguem `feature/<capacidade>`, `fix/<problema>` e `refactor/<contexto>`. Commits usam Conventional Commits e cada commit deve manter o repositório verificável. Exemplos para esta fase:

- `docs(architecture): define service boundaries and data ownership`
- `docs(events): specify checkout saga and event contracts`
- `docs(decisions): add initial architecture decision records`
