# Checkout e Saga

## Estratégia

O Order Service orquestra uma Saga baseada em mensagens. A escolha torna estado, deadlines e compensações explícitos, além de facilitar a demonstração de falhas. Não há transação distribuída nem chamada HTTP longa esperando estoque e pagamento.

## Fluxo de entrada

1. Cliente autenticado envia `POST /api/v1/checkout` com `Idempotency-Key`, carrinho e endereços selecionados.
2. Order Service valida ownership, estado do carrinho e formato.
3. Order consulta Catalog de forma síncrona para recalcular preços, promoções e cupom. O valor vindo do navegador nunca é confiável.
4. Em uma transação local, Order cria snapshots, pedido `CREATED`, registro `CheckoutSaga` e Outbox com `ReserveInventory`.
5. A API responde `202 Accepted` com `orderId`, estado e URL de acompanhamento.
6. O relay publica o comando e a Saga prossegue de forma assíncrona.

Se Catalog estiver indisponível antes do passo 4, nenhum pedido é criado e a API retorna erro transitório. Depois do commit, retries e compensações pertencem à Saga.

## Caminho de sucesso

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant W as Web
    participant O as Order Service
    participant K as Kafka
    participant I as Inventory Service
    participant P as Payment Service
    participant D as Demais consumidores

    C->>W: confirma checkout
    W->>O: POST /checkout + Idempotency-Key
    O->>O: valida, recalcula e grava Order + Saga + Outbox
    O-->>W: 202 + orderId (CREATED)
    O->>K: ReserveInventory
    K->>I: ReserveInventory
    I->>I: reserva atômica + Outbox
    I->>K: InventoryReserved
    K->>O: InventoryReserved
    O->>O: estado PAYMENT_PENDING + Outbox
    O->>K: RequestPayment
    K->>P: RequestPayment
    P->>P: tentativa idempotente + Outbox
    P->>K: PaymentApproved
    K->>O: PaymentApproved
    O->>O: estado PAID + Saga COMPLETED + Outbox
    O->>K: OrderConfirmed
    K->>D: atualiza Customer 360, CRM, analytics e notificações
    W->>O: GET /orders/{orderId}
    O-->>W: PAID
```

## Falha de pagamento e compensação

```mermaid
sequenceDiagram
    autonumber
    participant O as Order Service
    participant K as Kafka
    participant I as Inventory Service
    participant P as Payment Service

    O->>K: RequestPayment
    K->>P: RequestPayment
    P->>K: PaymentFailed
    K->>O: PaymentFailed
    O->>O: Saga COMPENSATING + Outbox
    O->>K: ReleaseInventory
    K->>I: ReleaseInventory
    I->>I: libera reserva uma única vez
    I->>K: InventoryReleased
    K->>O: InventoryReleased
    O->>O: Order CANCELLED + Saga COMPENSATED + Outbox
    O->>K: OrderCancelled
```

Se a reserva falhar, Order cancela diretamente sem solicitar pagamento. Se um pagamento for aprovado após um timeout que já iniciou compensação, a reconciliação ordena reembolso idempotente antes de concluir o cancelamento.

## Estados

### Pedido

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> PAYMENT_PENDING: estoque reservado
    CREATED --> CANCELLED: estoque insuficiente/expiração
    PAYMENT_PENDING --> PAID: pagamento aprovado
    PAYMENT_PENDING --> CANCELLED: pagamento falhou + estoque liberado
    PAID --> PROCESSING
    PROCESSING --> SHIPPED
    SHIPPED --> DELIVERED
    PAID --> REFUNDED: cancelamento/reembolso permitido
    PROCESSING --> REFUNDED: reembolso permitido
```

### Saga

`STARTED → RESERVING_INVENTORY → REQUESTING_PAYMENT → COMPLETED`

Rotas de falha usam `COMPENSATING → COMPENSATED`. Falhas operacionais repetidas usam `REQUIRES_ATTENTION`, geram alerta e preservam dados suficientes para retomada segura.

## Garantias e deadlines

- checkout repetido com a mesma chave e mesmo payload retorna o mesmo pedido;
- mesma chave com payload diferente retorna `409`;
- reserva e pagamento são únicos por pedido/operação;
- cada transição valida a versão atual da Saga;
- mensagens duplicadas não repetem movimentações;
- reserva possui expiração; um job publica o fato de expiração de forma idempotente;
- timeout não equivale automaticamente a falha definitiva de pagamento: o Payment Service reconcilia o estado da tentativa;
- DLQ não conclui nem cancela silenciosamente uma Saga; gera estado operacional visível.

## Dados observáveis

Order guarda timestamps de cada etapa, último evento, motivo de falha, tentativas e `correlationId`. Métricas medem duração ponta a ponta, taxa de compensação, Sagas paradas e divergências de reconciliação.
