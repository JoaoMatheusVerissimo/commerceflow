# ADR-013 — Pedido com snapshot e reserva concorrente de estoque

- Status: aceito para a Fase 4 autorizada pelo usuário
- Data: 2026-09-19

## Contexto

A Fase 4 exige pedidos, status, histórico, estoque e reserva, preservando os limites definidos na Fase 0. Ainda não existem Payment Service, Kafka ou compensação. Era necessário entregar uma fatia real sem fingir os componentes das fases seguintes e sem permitir venda da última unidade para dois pedidos concorrentes.

## Decisões

1. **Order permanece no Order Service e Inventory ganha serviço/banco próprios.** `order_db` recebe pedidos, itens e comandos de checkout; `inventory_db` recebe saldos, reservas, itens de reserva e movimentos. Não existem FKs ou acesso cruzado entre bancos.
2. **Pedido grava snapshot comercial imutável.** Nome, slug, SKU, quantidade, preço unitário e total vêm de uma cotação nova do Catalog. Mudanças posteriores no catálogo não alteram o pedido.
3. **Checkout é idempotente por cliente e chave.** A chave aponta para um único pedido e guarda hash da versão do carrinho. Mesmo corpo retorna o pedido existente; corpo divergente retorna 409. A gravação `CREATED` antecede a chamada remota para permitir retomada segura.
4. **Reserva é idempotente por pedido e conjunto de itens.** Inventory usa `orderId` como identidade da reserva. Repetição igual devolve a reserva; repetição divergente retorna conflito.
5. **Concorrência usa locks pessimistas curtos e defesa condicional.** SKUs são ordenados para evitar ordem de lock inconsistente, carregados com `SELECT ... FOR UPDATE`, validados em conjunto e atualizados apenas se `physical - reserved` ainda comportar a quantidade. Nenhum efeito parcial é persistido quando um item falha.
6. **Integração HTTP síncrona é um precursor explícito.** Order delega o JWT CUSTOMER e correlation ID para Catalog e Inventory. Timeouts/falhas transitórias retornam 503, mantêm `CREATED` e exigem replay da mesma chave. Na Fase 6, a interação apropriada migra para a Saga/Kafka definida nos ADRs 002, 004 e 005.
7. **Estados produzidos nesta fase são limitados.** `CREATED`, `PAYMENT_PENDING` após reserva e `CANCELLED` por estoque insuficiente. `PAYMENT_PENDING` significa somente estoque reservado; nenhuma cobrança é executada. O carrinho é apagado apenas após a reserva confirmada.
8. **Disponibilidade pública não é comando.** `GET /availability/{sku}` expõe números demonstrativos do seed; movimentos e mínimos exigem roles operacionais. Movimentos registram auditoria local antes/depois, ator e referência.
9. **PostgreSQL é a prova de concorrência.** Testes H2 cobrem regras rápidas, mas reserva da última unidade, migrations e tipos temporais são verificados também com Testcontainers/PostgreSQL. O smoke do Compose valida a cadeia frontend→gateway→serviços→bancos.

## Consequências e evolução

- A indisponibilidade do Inventory pode deixar um pedido `CREATED`; o replay idempotente é a recuperação nesta fase. Reconciliação automática e expiração chegam com o workflow de pagamentos/Saga.
- Reserva ativa reduz disponibilidade, mas ainda não possui liberação ou expiração automática. Isso é um limite conhecido, não uma capacidade simulada.
- Paginação offset e consulta de itens por pedido são adequadas ao volume demonstrativo; otimizações dependem de medição.
- A criação concorrente de um SKU inédito via movimento administrativo depende da restrição única do banco; o caminho normal opera em SKUs previamente cadastrados pelo seed/admin.

Execução e evidências: [Fase 4](../orders.md).
