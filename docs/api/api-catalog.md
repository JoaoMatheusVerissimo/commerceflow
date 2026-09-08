# Catálogo inicial de APIs

## Convenções HTTP

- prefixo externo `/api/v1` e versionamento independente nas especificações internas;
- JSON em `camelCase`, timestamps ISO 8601 UTC e valores monetários decimais como string mais moeda;
- `Idempotency-Key` obrigatório em checkout, pagamento e reembolso;
- `X-Correlation-Id` aceito quando válido ou criado no gateway e sempre devolvido;
- paginação cursor-based em timelines/feed e page-based em tabelas administrativas quando ordenação total for estável;
- filtros com allowlist e limites máximos de página;
- resposta de erro conforme [contracts/http/error-response.schema.json](../../contracts/http/error-response.schema.json);
- OpenAPI por serviço será executável e validado na fase em que o endpoint for implementado.

Nas tabelas abaixo, `SELLER+` inclui SELLER, MANAGER e ADMIN; `MANAGER+` inclui MANAGER e ADMIN.

## Auth Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `POST /auth/register` | público | criar conta CUSTOMER e iniciar criação do perfil |
| `POST /auth/login` | público | autenticar e criar sessão rotativa |
| `POST /auth/refresh` | refresh cookie | rotacionar refresh token e emitir access token |
| `POST /auth/logout` | autenticado | revogar sessão atual |
| `POST /auth/logout-all` | autenticado | revogar todas as sessões do usuário |
| `GET /auth/me` | autenticado | retornar identidade e permissões efetivas |
| `GET /auth/.well-known/jwks.json` | serviços | publicar chaves públicas ativas |
| `GET/POST/PATCH /admin/users...` | ADMIN | administrar estado e roles de acesso |

## Customer Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `GET/PATCH /customers/me` | CUSTOMER | consultar/alterar o próprio perfil |
| `GET/POST/PATCH/DELETE /customers/me/addresses...` | CUSTOMER | gerenciar endereços próprios |
| `GET/PUT /customers/me/preferences` | CUSTOMER | preferências e consentimentos |
| `GET/POST/DELETE /customers/me/favorites...` | CUSTOMER | gerenciar favoritos |
| `GET /admin/customers` | SELLER+ | busca paginada de clientes autorizados |
| `GET /admin/customers/{customerId}` | SELLER+ | Customer 360 agregado |
| `GET /admin/customers/{customerId}/timeline` | SELLER+ | timeline paginada |
| `POST/DELETE /admin/customers/{customerId}/labels...` | MANAGER+ | labels operacionais |
| `POST /customers/me/data-export` | CUSTOMER | iniciar exportação LGPD |
| `POST /customers/me/anonymization` | CUSTOMER | solicitar workflow de anonimização |

## Catalog Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `GET /products` | público | catálogo com busca, categoria, preço e paginação |
| `GET /products/{slug}` | público | detalhe e variações do produto |
| `POST /products/{productId}/views` | público com sessão limitada | registrar visualização assíncrona sem bloquear a navegação |
| `GET /categories` | público | categorias navegáveis |
| `GET /categories/{slug}/products` | público | produtos da categoria |
| `GET/POST /products/{productId}/reviews` | leitura pública/escrita CUSTOMER | avaliações moderáveis |
| `POST /internal/pricing/quote` | Order | cotação autoritativa de itens e cupom |
| `GET/POST/PATCH /admin/products...` | MANAGER+ | gestão de produtos e variantes |
| `GET/POST/PATCH /admin/categories...` | MANAGER+ | gestão de categorias |
| `GET/POST/PATCH /admin/coupons...` | MANAGER+ | gestão de cupons e promoções |

## Inventory Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `GET /products/{productId}/availability` | público | disponibilidade agregada sem expor saldo sensível |
| `GET /admin/inventory` | SELLER+ | consultar saldos e alertas |
| `POST /admin/inventory/{sku}/movements` | MANAGER+ | entrada/saída/ajuste auditável |
| `PATCH /admin/inventory/{sku}/minimum` | MANAGER+ | definir estoque mínimo |

Reserva e liberação do checkout ocorrem por comandos Kafka, não por endpoint público.

## Order Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `GET/PUT /cart` | sessão/cliente | obter e atualizar carrinho atual |
| `POST/PATCH/DELETE /cart/items...` | sessão/cliente | gerenciar itens |
| `PUT/DELETE /cart/coupon` | sessão/cliente | aplicar ou remover cupom |
| `POST /checkout` | CUSTOMER | criar pedido idempotente e iniciar Saga; retorna `202` |
| `GET /orders` | CUSTOMER | histórico próprio |
| `GET /orders/{orderId}` | dono ou staff | detalhe e estado da Saga/pedido |
| `POST /orders/{orderId}/cancel` | dono conforme estado ou staff | solicitar cancelamento |
| `GET /admin/orders` | SELLER+ | consulta operacional |
| `PATCH /admin/orders/{orderId}/fulfillment` | SELLER+ | transições de processamento/entrega permitidas |

## Payment Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `GET /admin/payments/{paymentId}` | MANAGER+ | inspeção sanitizada da tentativa |
| `POST /admin/payments/{paymentId}/refunds` | MANAGER+ | reembolso idempotente |
| `PUT /admin/payment-simulator/scenario` | ADMIN, demo apenas | configurar aprovação, recusa, timeout ou erro |

Solicitação do pagamento da Saga ocorre por Kafka. Nenhum dado real de cartão é armazenado.

## CRM Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `GET/POST/PATCH /admin/crm/pipelines...` | MANAGER+ | configurar pipelines e estágios |
| `GET/POST/PATCH /admin/crm/leads...` | SELLER+ | gerenciar leads |
| `GET/POST/PATCH /admin/crm/opportunities...` | SELLER+ | gerenciar oportunidades |
| `POST /admin/crm/opportunities/{id}/move` | SELLER+ | mover card com controle de versão |
| `GET/POST/PATCH /admin/crm/tasks...` | SELLER+ | tarefas e follow-ups |
| `GET/POST /admin/crm/interactions...` | SELLER+ | registrar contatos |
| `GET/POST /admin/crm/notes...` | SELLER+ | notas autorizadas |
| `GET /admin/audit` | MANAGER+ | projeção consolidada de auditoria |

## Notification Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `GET /notifications` | autenticado | notificações do usuário |
| `PATCH /notifications/{id}/read` | destinatário | marcar como lida |
| `GET/PUT /notification-preferences` | autenticado | preferências por canal/categoria |
| `GET /admin/notifications` | MANAGER+ | monitorar entregas permitidas |

WebSocket será adicionado somente na Fase 12; até lá, a API permite consulta.

## Analytics Service

| Método e rota | Acesso | Finalidade |
|---|---|---|
| `GET /admin/analytics/dashboard` | MANAGER+ | métricas agregadas por período |
| `GET /admin/analytics/segments` | MANAGER+ | distribuição RFM e segmentos |
| `GET /admin/analytics/customers/{id}` | SELLER+ | resultado explicável do cliente |
| `GET /recommendations` | CUSTOMER | recomendações do usuário autenticado |
| `POST /internal/analytics/recompute` | job/admin controlado | recomputação versionada |

## Semântica de status relevante

- `200/201/204`: sucesso síncrono;
- `202`: workflow assíncrono iniciado;
- `400`: sintaxe/formato inválido;
- `401`: identidade ausente ou inválida;
- `403`: identidade válida sem permissão/ownership;
- `404`: recurso inexistente ou ocultado por segurança;
- `409`: conflito de estado, versão ou idempotência;
- `422`: regra de negócio não atendida;
- `429`: rate limit;
- `503`: dependência essencial indisponível antes de iniciar o workflow.
