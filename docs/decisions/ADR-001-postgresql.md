# ADR-001 — PostgreSQL como banco transacional principal

- Status: Aceito
- Data: 2026-09-08

## Contexto

Pedidos, estoque, pagamentos, clientes e CRM exigem constraints, transações, índices e consultas relacionais. A equipe também precisa executar o projeto localmente com baixo custo operacional.

## Decisão

Usar PostgreSQL como persistência transacional padrão. Cada serviço possui database, usuário e migrations próprios. No ambiente local, databases compartilham uma instância; o isolamento físico pode evoluir sem compartilhar tabelas.

Analytics também inicia em PostgreSQL. Outro mecanismo só será adotado quando volume e consultas comprovarem a necessidade.

## Consequências

- integridade local forte e tooling maduro;
- Testcontainers pode validar migrations, queries e concorrência real;
- joins entre serviços são proibidos e exigem snapshots/projeções;
- backups e migrations são responsabilidade de cada serviço;
- compartilhar uma instância local não significa compartilhar ownership.

## Alternativas consideradas

- Um único schema compartilhado: rejeitado por acoplar deploys e ownership.
- Bancos diferentes por serviço desde o início: rejeitado por complexidade sem benefício demonstrado.
