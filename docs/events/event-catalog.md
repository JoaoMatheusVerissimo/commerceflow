# Catálogo de eventos e comandos

## Convenções

Eventos são fatos no passado (`OrderCreated`); comandos são solicitações no imperativo (`ReserveInventory`). Ambos usam o envelope em [contracts/events/event-envelope.schema.json](../../contracts/events/event-envelope.schema.json), mas trafegam em tópicos separados.

Nomes físicos seguem:

- `commerceflow.<dominio>.events.v1`
- `commerceflow.<dominio>.commands.v1`
- `commerceflow.<dominio>.dlq.v1`

A chave Kafka é o `aggregateId`, preservando ordem por agregado. A versão do tópico representa uma quebra ampla do canal; `eventVersion` versiona o contrato individual. Novos campos opcionais são compatíveis. Remoção, mudança de significado ou tipo exige nova major version.

## Envelope obrigatório

| Campo | Uso |
|---|---|
| `eventId` | UUID único usado na deduplicação |
| `eventType` | nome estável do contrato |
| `eventVersion` | versão semântica do payload |
| `timestamp` | instante UTC em que o fato ocorreu |
| `correlationId` | correlaciona a jornada iniciada por HTTP ou evento |
| `causationId` | evento/comando que causou a mensagem atual, quando existir |
| `producer` | serviço que publicou |
| `aggregateId` | chave do agregado e da partição |
| `payload` | dados mínimos necessários, sem informações secretas |

Headers transportam W3C `traceparent`/`tracestate`, content type e schema reference. `correlationId` é identidade de negócio; `traceId` é identidade de uma execução observada e os dois não são intercambiáveis.

## Eventos de domínio

| Evento | Produtor | Consumidores principais | Payload mínimo |
|---|---|---|---|
| `UserCreated` | Auth | Customer | `userId`, `email`, `occurredAt` |
| `CustomerCreated` | Customer | CRM, Analytics | `customerId`, `userId`, `createdAt` |
| `ProductViewed` | Catalog | Customer, Analytics | `customerId?`, `sessionId`, `productId`, `viewedAt` |
| `ProductAddedToCart` | Order | Customer, Analytics | `cartId`, `customerId?`, `productId`, `sku`, `quantity` |
| `CartAbandoned` | Order | CRM, Notification, Analytics | `cartId`, `customerId`, `value`, `abandonedAt` |
| `OrderCreated` | Order | Customer, CRM, Analytics | `orderId`, `customerId`, `total`, `currency`, `itemCount` |
| `InventoryReserved` | Inventory | Order | `orderId`, `reservationId`, `expiresAt` |
| `InventoryReservationFailed` | Inventory | Order | `orderId`, `reason`, `unavailableSkus` |
| `InventoryReleased` | Inventory | Order | `orderId`, `reservationId`, `reason` |
| `PaymentRequested` | Payment | Order, audit projection | `paymentId`, `orderId`, `amount`, `currency` |
| `PaymentApproved` | Payment | Order | `paymentId`, `orderId`, `approvedAt` |
| `PaymentFailed` | Payment | Order, Notification | `paymentId`, `orderId`, `reasonCode`, `retryable` |
| `PaymentRefunded` | Payment | Order, Customer | `paymentId`, `orderId`, `amount`, `refundedAt` |
| `OrderConfirmed` | Order | Customer, CRM, Analytics, Notification | `orderId`, `customerId`, `total`, `confirmedAt` |
| `OrderCancelled` | Order | Customer, CRM, Analytics, Notification | `orderId`, `customerId`, `reasonCode`, `cancelledAt` |
| `OrderShipped` | Order | Customer, Notification | `orderId`, `customerId`, `shippedAt`, `trackingCode?` |
| `OrderDelivered` | Order | Customer, Analytics, Notification | `orderId`, `customerId`, `deliveredAt` |
| `LowStockDetected` | Inventory | Notification, CRM | `sku`, `available`, `minimum`, `detectedAt` |
| `CustomerScoreUpdated` | Customer | CRM, Analytics | `customerId`, `score`, `classification`, `ruleVersion` |
| `CustomerSegmentChanged` | Customer | CRM, Notification | `customerId`, `previousSegment`, `newSegment`, `ruleVersion` |
| `CustomerBecameVip` | Customer | CRM, Notification | `customerId`, `score`, `qualifiedAt` |
| `ChurnRiskDetected` | Analytics | Customer, CRM, Notification | `customerId`, `risk`, `classification`, `reasonCodes`, `modelVersion` |
| `AdministrativeActionRecorded` | qualquer serviço de escrita | CRM | `actorId`, `action`, `resource`, `resourceId`, `occurredAt`, `changes` sanitizado |

Valores monetários usam decimal serializado como string e código ISO de moeda, evitando perda de precisão. Eventos não incluem password hash, token, segredo, endereço completo ou dados de pagamento sensíveis.

## Comandos da Saga

| Comando | Produtor | Destino lógico | Resultado esperado |
|---|---|---|---|
| `ReserveInventory` | Order | Inventory | `InventoryReserved` ou `InventoryReservationFailed` |
| `ReleaseInventory` | Order | Inventory | `InventoryReleased` |
| `RequestPayment` | Order | Payment | `PaymentApproved` ou `PaymentFailed` |
| `CancelPayment` | Order | Payment | cancelamento ou resultado idempotente atual |
| `RefundPayment` | Order | Payment | `PaymentRefunded` ou falha operacional tratada |

## Entrega, retry e idempotência

Kafka oferece entrega pelo menos uma vez nesta arquitetura. Portanto:

1. o produtor grava agregado e Outbox na mesma transação;
2. um relay publica a Outbox e marca a tentativa, sem presumir exactly-once entre banco e broker;
3. o consumidor inicia transação local, reserva `eventId` em Inbox, aplica o efeito e confirma;
4. duplicatas encontram a chave única e retornam sem repetir o efeito;
5. falhas transitórias usam retry com backoff e limite;
6. falhas permanentes seguem para DLQ com metadados, sem copiar secrets;
7. replay é uma operação auditada e mantém o mesmo `eventId` quando reprocessa a mesma mensagem.

Handlers devem ser idempotentes também por chave de negócio: reserva por `orderId + sku`, pagamento por `orderId + operationType` e oportunidade automática por `customerId + reason + sourceId`.

## Ordenação e evolução

- ordenação global não é assumida;
- cada agregado possui `aggregateVersion`; versões antigas ou repetidas não sobrescrevem estado novo;
- consumidores toleram campos desconhecidos;
- mudanças incompatíveis mantêm produtor e consumidor em período de convivência;
- schemas executáveis de cada payload serão adicionados junto com o serviço produtor e validados em CI.
