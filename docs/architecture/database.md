# Estratégia de dados

## Database per service

| Database | Serviço | Dados principais |
|---|---|---|
| `auth_db` | Auth | contas, roles, permissões, sessões de refresh |
| `customer_db` | Customer | clientes, endereços, consentimentos, favoritos, labels, timeline, métricas |
| `catalog_db` | Catalog | produtos, variantes, categorias, preços, promoções, cupons, avaliações |
| `inventory_db` | Inventory | saldos, reservas e movimentações |
| `order_db` | Order | carrinhos, pedidos, itens, saga e snapshots comerciais |
| `payment_db` | Payment | pagamentos, tentativas, reembolsos |
| `crm_db` | CRM | leads, oportunidades, pipelines, interações, tarefas, notas e auditoria projetada |
| `notification_db` | Notification | notificações, preferências e tentativas de entrega |
| `analytics_db` | Analytics | features, resultados RFM/churn, recomendações e agregados |

Na execução local, uma instância PostgreSQL hospeda databases isolados e usuários com privilégios mínimos. Um serviço não recebe credenciais para outro database.

## Padrões comuns

Serviços conectados ao Kafka mantêm:

- `outbox_event`: fato persistido na mesma transação do agregado e publicado posteriormente;
- `inbox_event` ou `processed_event`: `eventId` consumido, handler e resultado, com unicidade para idempotência;
- migrations imutáveis e executadas pelo próprio serviço;
- timestamps em UTC e IDs UUID gerados pela aplicação ou banco.

Essas tabelas possuem formato conceitual comum, mas migrations locais. Não há schema SQL central compartilhado.

## Consistência e concorrência

- Order usa lock/versionamento otimista para transições e uma chave idempotente por checkout.
- Inventory executa a condição `available >= requested` e a reserva de forma atômica. A implementação inicial deverá usar update condicional ou lock pessimista curto e possuir teste concorrente com Testcontainers.
- Payment impõe unicidade em `(order_id, operation_type)` e na idempotency key.
- Consumers registram o evento processado na mesma transação de seus efeitos locais.
- Snapshots preservam nome, SKU, preço, desconto e endereço que valeram na compra.

## Índices iniciais orientados a consultas

- e-mail normalizado único em Auth e Customer;
- produto por `slug`, SKU único e busca por status/categoria;
- estoque por SKU; reservas por pedido e estado/expiração;
- pedidos por cliente/data e status/data;
- oportunidades por pipeline/estágio/ordem e responsável/follow-up;
- timeline por cliente/data decrescente;
- outbox por estado/data e inbox por `event_id` único.

Índices adicionais devem nascer de queries reais e métricas, não de suposição.

## Privacidade e retenção

- dados pessoais ficam concentrados no Customer Service sempre que possível;
- Auth armazena apenas o necessário para identidade e segurança;
- logs, eventos e analytics usam IDs, evitando e-mail, telefone e endereço;
- exportação, anonimização e exclusão são workflows auditáveis;
- pedidos podem reter snapshots exigidos por integridade comercial, com campos pessoais minimizados ou anonimizados conforme política documentada;
- políticas exatas de retenção serão configuráveis e documentadas antes de uma implantação real.

## Backup e recuperação

RPO/RTO de produção não são alegados nesta fase. A estratégia futura deve testar restauração, preservar ordem de migrations e considerar a recuperação coordenada de bancos e offsets/projeções Kafka.
