# Visão geral do sistema

## Objetivo

CommerceFlow combina uma loja virtual e um backoffice de CRM/analytics. O desenho separa contextos com regras e ciclos de mudança próprios, mantendo integrações explícitas por HTTP ou eventos.

## Visão de containers

```mermaid
flowchart TB
    customer[Cliente] --> web[React Web]
    staff[Equipe interna] --> web
    web -->|HTTPS / JSON| gateway[Spring API Gateway]

    gateway --> auth[Auth Service]
    gateway --> customerSvc[Customer Service]
    gateway --> catalog[Catalog Service]
    gateway --> inventory[Inventory Service]
    gateway --> orders[Order Service]
    gateway --> payments[Payment Service]
    gateway --> crm[CRM Service]
    gateway --> notifications[Notification Service]
    gateway --> analytics[Analytics Service]

    orders -->|comandos e eventos| kafka[(Kafka)]
    inventory --> kafka
    payments --> kafka
    auth --> kafka
    customerSvc --> kafka
    catalog --> kafka
    crm --> kafka
    notifications --> kafka
    analytics --> kafka

    auth --- authDb[(auth_db)]
    customerSvc --- customerDb[(customer_db)]
    catalog --- catalogDb[(catalog_db)]
    inventory --- inventoryDb[(inventory_db)]
    orders --- orderDb[(order_db)]
    payments --- paymentDb[(payment_db)]
    crm --- crmDb[(crm_db)]
    notifications --- notificationDb[(notification_db)]
    analytics --- analyticsDb[(analytics_db)]

    catalog --- redis[(Redis)]
    gateway -. rate limit .-> redis

    web -. telemetry .-> otel[OpenTelemetry Collector]
    gateway -. telemetry .-> otel
    kafka -. telemetry .-> otel
    auth -. telemetry .-> otel
    customerSvc -. telemetry .-> otel
    catalog -. telemetry .-> otel
    inventory -. telemetry .-> otel
    orders -. telemetry .-> otel
    payments -. telemetry .-> otel
    crm -. telemetry .-> otel
    notifications -. telemetry .-> otel
    analytics -. telemetry .-> otel
```

## Estilos de comunicação

- **HTTP síncrono:** autenticação, consultas de tela, comandos que precisam de validação imediata e operações administrativas.
- **Kafka assíncrono:** mudanças de estado entre contextos, atualização de projeções, automações, notificações, analytics e Saga de checkout.
- **Banco por serviço:** nenhuma consulta direta ao schema de outro contexto. Dados externos necessários à leitura são copiados como snapshot ou projeção.
- **Redis:** somente cache de catálogo e rate limiting do gateway inicialmente; não é fonte de verdade.

## Consistência

Transações ACID terminam dentro de um serviço. Entre serviços, o sistema usa consistência eventual, Transactional Outbox e consumidores idempotentes. O usuário recebe um identificador de pedido e acompanha a evolução do checkout, em vez de manter uma transação HTTP aberta durante toda a Saga.

## Topologia local e evolução

No ambiente local, os bancos lógicos podem compartilhar uma instância PostgreSQL, mas possuem database, usuário e migrations separados. Isso preserva ownership sem pagar o custo operacional de múltiplos servidores. Em produção, qualquer banco poderá ser isolado sem alterar contratos.

Os serviços serão introduzidos nas fases em que exista um caso de uso vertical para exercitá-los. A arquitetura alvo não autoriza gerar todos os containers antecipadamente.
