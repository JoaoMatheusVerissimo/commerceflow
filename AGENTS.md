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

Fase 0 — Arquitetura. Entregáveis exigidos antes de implementação funcional:

Status: documentação concluída em 2026-09-08 e aguardando revisão/aprovação explícita do usuário.

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

Após concluir esses itens, aguarde a aprovação do usuário antes da Fase 1.
