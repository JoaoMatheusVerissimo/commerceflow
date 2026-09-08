# ADR-005 — Saga de checkout orquestrada

- Status: Aceito
- Data: 2026-09-08

## Contexto

Confirmar um pedido depende de reserva de estoque e pagamento, pertencentes a bancos e serviços diferentes. Falhas, timeout e mensagens duplicadas precisam resultar em compensações compreensíveis.

## Decisão

Order Service mantém uma máquina de estado `CheckoutSaga` e envia comandos a Inventory e Payment pelo Kafka. Resultados avançam a Saga; falha de pagamento solicita liberação de estoque; aprovação tardia após timeout pode exigir reembolso.

A API cria o pedido e retorna `202`; o cliente acompanha o estado. O fluxo completo está em [checkout-and-saga.md](../architecture/checkout-and-saga.md).

## Consequências

- estado, deadlines e compensações ficam centralizados e auditáveis;
- Order ganha responsabilidade operacional adicional;
- consumidores continuam independentes e idempotentes;
- Sagas paradas exigem métrica, reconciliação e intervenção visível;
- não há rollback global: compensações são novas ações de negócio.

## Alternativas consideradas

- Choreography pura: rejeitada para o checkout porque torna o fluxo crítico e suas compensações mais difíceis de visualizar.
- Transação distribuída: rejeitada por acoplamento e baixa resiliência.
