# CommerceFlow — instruções permanentes do projeto

O documento `docs/PROJECT-BRIEF.md` é a fonte de verdade do produto e da arquitetura desejada. Antes de planejar ou alterar código neste repositório, leia-o integralmente e confronte o trabalho solicitado com ele.

## Regras operacionais obrigatórias

- Atue como Senior Software Engineer e Software Architect.
- Desenvolva de forma progressiva, profissional e verificável; não gere o sistema inteiro de uma vez.
- Respeite rigorosamente o roadmap por fases. O projeto começa exclusivamente pela Fase 0 — Arquitetura. Não inicie a Fase 1 sem aprovação explícita do usuário.
- Em cada fase: explique o objetivo; analise decisões arquiteturais; informe arquivos afetados; implemente completamente; adicione e execute testes relevantes; corrija falhas; atualize documentação; sugira commits; e resuma o concluído.
- Verifique o estado atual do repositório antes de qualquer mudança e preserve o que já funciona.
- Não entregue código quebrado, funcionalidades fictícias, arquivos vazios, mocks que finjam capacidades, placeholders finais ou TODOs críticos.
- Não prometa no README recursos ainda inexistentes.
- Priorize qualidade, legibilidade, segurança, privacidade, testabilidade, baixo acoplamento, alta coesão e manutenção.
- Use padrões e dependências apenas quando resolverem problemas reais; prefira regras explicáveis a ML desnecessário.
- Nunca versione senhas, tokens, API keys ou outros segredos.
- Considere uma feature pronta somente conforme a Definition of Done do briefing.
- Faça a evolução visual nesta ordem: loja funcional → painel administrativo → CRM Kanban → Customer 360 → Analytics → observabilidade.

## Fase atual

Fase 4 — Orders, autorizada explicitamente pelo usuário após o Cart.

Status: Fase 4 implementada e validada localmente em 2026-09-19: 60 testes Java, 25 testes frontend, lint/build, auditoria npm e smokes Foundation/Catalog/Cart/Orders aprovados, incluindo concorrência em PostgreSQL real e Compose. O aceite remoto aguarda commit/push e CI verde. Fases 1–3 preservadas; a Fase 3 tem CI verde no commit `e3c8d22`. Não avançar à Fase 5 sem nova aprovação explícita.

Entregáveis arquiteturais da Fase 0 preservados:

1. Arquitetura geral
2. Diagrama
3. Limites dos microsserviços
4. Estrutura do monorepo
5. Entidades principais
6. Bancos de dados
7. Eventos Kafka
8. APIs principais
9. Fluxo completo de checkout
10. Fluxo Saga
11. Estratégia de autenticação
12. Estratégia de observabilidade
13. ADRs iniciais
14. Roadmap técnico

Consulte `docs/orders.md` para execução, APIs e evidências atuais, ADR-013 para pedidos/reserva, `docs/cart.md`/ADR-012 para carrinho, `docs/catalog.md`/ADR-011 para catálogo e `docs/foundation.md`/ADR-010 para a base.
