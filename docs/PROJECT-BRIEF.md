# COMMERCEFLOW

## Plataforma Full-Stack de E-commerce + CRM + Customer Analytics

Quero desenvolver um projeto profissional para meu portfólio chamado **CommerceFlow**.

O objetivo não é criar apenas um e-commerce tradicional.

Quero construir uma plataforma inspirada em sistemas reais de comércio digital, integrando:

* E-commerce
* CRM
* Gestão de clientes
* Gestão de pedidos
* Gestão de estoque
* Pagamentos
* Automação comercial
* Customer Analytics
* Segmentação de clientes
* Notificações
* Arquitetura orientada a eventos
* Observabilidade
* DevOps

O projeto deverá demonstrar conhecimento avançado de:

* Java
* Spring Boot
* Python
* FastAPI
* React
* TypeScript
* PostgreSQL
* Redis
* Kafka
* Docker
* Microsserviços
* Arquitetura orientada a eventos
* CI/CD
* Testes
* Observabilidade

---

# 1. OBJETIVO DO PROJETO

Construir uma aplicação SaaS chamada:

**CommerceFlow**

Descrição:

> Event-driven e-commerce and CRM platform built with Java, Spring Boot, Kafka, Python, React and TypeScript.

Descrição expandida:

> A production-inspired commerce platform combining e-commerce, CRM, customer analytics, inventory management, payments and event-driven automation.

Quero que o sistema simule uma plataforma comercial utilizada por uma empresa real.

Deverão existir dois ambientes principais.

---

# 2. AMBIENTE 1 — E-COMMERCE

Área acessada pelos clientes da loja.

Deve possuir:

* Página inicial
* Catálogo
* Busca
* Categorias
* Página de produto
* Variações de produto
* Carrinho
* Favoritos
* Checkout
* Cupons
* Conta do cliente
* Histórico de pedidos
* Acompanhamento de pedidos
* Avaliações
* Recomendações de produtos
* Carrinho abandonado

Fluxo básico:

VISITANTE

↓

CADASTRO / LOGIN

↓

PRODUTOS

↓

CARRINHO

↓

CHECKOUT

↓

PAGAMENTO

↓

PEDIDO

↓

ENTREGA

---

# 3. AMBIENTE 2 — ADMIN / CRM

Criar um painel administrativo completo.

Menu:

Dashboard

Clientes

CRM

Pedidos

Produtos

Categorias

Estoque

Cupons

Campanhas

Analytics

Notificações

Audit Logs

Configurações

---

# 4. DASHBOARD EXECUTIVO

Criar uma interface moderna de SaaS.

Principais indicadores:

* Receita
* Total de pedidos
* Clientes
* Ticket médio
* Taxa de conversão
* Pedidos pendentes
* Pedidos cancelados
* Produtos com estoque baixo
* Clientes em risco
* Carrinhos abandonados
* Clientes recorrentes

Exemplo:

REVENUE
R$ 184.320
↑ 12.4%

ORDERS
1.284
↑ 8.7%

CUSTOMERS
842
↑ 21%

AVERAGE TICKET
R$ 437
↑ 4.3%

Criar gráficos de:

* Receita por período
* Pedidos por período
* Novos clientes
* Produtos mais vendidos
* Categorias mais vendidas
* Customer segmentation
* Ticket médio
* Conversão

---

# 5. CUSTOMER 360

Essa será uma das principais funcionalidades.

Cada cliente deverá possuir uma tela completa.

Exemplo:

Maria Oliveira

Status:
Cliente recorrente

Customer Score:
92 / 100

Total gasto:
R$ 4.850

Pedidos:
12

Ticket médio:
R$ 404

Última compra:
03/09/2026

Produtos favoritos:

* Botas
* Chapéus

Tags:

* VIP
* Cliente recorrente
* Country feminino

A tela deve conter:

* Dados pessoais
* Endereços
* Histórico de pedidos
* Total gasto
* Ticket médio
* Frequência de compra
* Produtos favoritos
* Categorias favoritas
* Tags
* Score
* Segmentação
* Timeline
* Anotações
* Tarefas
* Oportunidades
* Interações
* Carrinhos abandonados

---

# 6. TIMELINE DO CLIENTE

Criar uma timeline de atividades.

Exemplo:

08/09/2026

Comprou Bota Western
R$ 649,00

---

05/09/2026

Adicionou Bota Western ao carrinho

---

03/09/2026

Visualizou Bota Western 4 vezes

---

27/08/2026

Criou sua conta

A timeline deverá ser alimentada automaticamente através dos eventos do sistema.

---

# 7. CRM

Criar um CRM integrado ao e-commerce.

Entidades:

Customer

Lead

Opportunity

Interaction

Note

Tag

Task

Pipeline

PipelineStage

---

# 8. PIPELINE COMERCIAL

Criar um Kanban drag-and-drop.

Exemplo:

NOVO LEAD

↓

EM CONTATO

↓

QUALIFICADO

↓

NEGOCIAÇÃO

↓

CLIENTE

↓

PÓS-VENDA

Permitir:

* mover cards
* criar oportunidades
* definir responsável
* definir valor
* criar tarefas
* adicionar notas
* registrar contatos
* definir data de follow-up
* adicionar tags

---

# 9. LEADS AUTOMÁTICOS

O CRM deverá criar oportunidades automaticamente dependendo do comportamento do usuário.

Exemplo:

Cliente visualizou o mesmo produto várias vezes.

Depois:

Adicionou ao carrinho.

Depois:

Abandonou o carrinho.

O sistema poderá criar:

POTENTIAL CUSTOMER

Maria Oliveira

Interest:
Bota Western Feminina

Reason:
Abandoned Cart

Potential Value:
R$ 699

Last Activity:
2 hours ago

---

# 10. AUTOMAÇÕES DO CRM

Criar um pequeno motor de automações.

Exemplos:

REGRA 1

SE

cliente realizar terceira compra

ENTÃO

adicionar tag:

"Cliente recorrente"

---

REGRA 2

SE

cliente gastar mais de R$ 5.000

ENTÃO

adicionar tag:

"VIP"

---

REGRA 3

SE

carrinho permanecer abandonado por 24 horas

ENTÃO

criar oportunidade no CRM

---

REGRA 4

SE

cliente ficar 90 dias sem comprar

ENTÃO

classificar como:

"At Risk"

---

REGRA 5

SE

estoque ficar abaixo do mínimo

ENTÃO

gerar alerta para administrador.

---

# 11. CUSTOMER SCORE

Criar um sistema de pontuação do cliente.

Escala:

0 – 100

Utilizar fatores como:

* frequência de compra
* ticket médio
* recência
* quantidade de pedidos
* devoluções
* carrinhos abandonados
* engajamento

Classificação:

90-100

VIP

75-89

Muito ativo

50-74

Ativo

25-49

Em risco

0-24

Inativo

A regra deverá ser documentada e testável.

---

# 12. RFM ANALYSIS

Criar um serviço em Python para Customer Analytics.

Utilizar:

R — Recency

F — Frequency

M — Monetary

Classificar clientes como:

* Champions
* Loyal Customers
* Potential Loyalists
* New Customers
* At Risk
* Hibernating
* Lost Customers

Tecnologias:

Python

FastAPI

Pandas

Scikit-learn, apenas quando realmente necessário.

Não utilizar Machine Learning simplesmente para adicionar IA ao projeto.

Priorizar regras estatísticas explicáveis antes de ML.

---

# 13. CHURN PREDICTION

Em uma etapa avançada do projeto, desenvolver uma análise de risco de churn.

Exemplo:

Maria Oliveira

Churn Risk

78%

Classification:

HIGH RISK

Motivos:

* 93 dias sem comprar
* redução da frequência de compra
* carrinho abandonado recentemente

O modelo deve possuir documentação explicando:

* dados utilizados
* features
* cálculo
* limitações
* avaliação

---

# 14. RECOMENDAÇÃO DE PRODUTOS

Criar sistema simples de recomendação.

Exemplo:

Clientes que compraram:

Bota Western

também compraram:

* Cinto Texas
* Chapéu Austin

Começar utilizando comportamento histórico.

Não utilizar LLM para recomendações.

---

# 15. ARQUITETURA

Arquitetura desejada:

Frontend

React + TypeScript

↓

API Gateway

↓

Microsserviços

Estrutura:

```
                   React
                     │
                     ▼
             Spring API Gateway
                     │
    ┌────────────────┼─────────────────┐
    │                │                 │
    ▼                ▼                 ▼
```

Auth Service     Customer Service   Catalog Service
│                │                 │
├────────────────┼─────────────────┤
│                │                 │
▼                ▼                 ▼
Order Service   Inventory Service    CRM Service
│                │                 │
└────────────────┼─────────────────┘
│
▼
Kafka
│
┌──────────────┼──────────────┐
│              │              │
▼              ▼              ▼
Payment       Notification    Analytics
Service         Service       Service
│
Python

---

# 16. MICROSSERVIÇOS

Não criar microsserviços apenas por aparência.

Cada serviço deve possuir responsabilidade clara.

Criar:

## auth-service

Tecnologia:

Java + Spring Boot

Responsável por:

* autenticação
* registro
* JWT
* refresh token
* roles
* permissões
* gerenciamento de usuários

Roles:

CUSTOMER

SELLER

MANAGER

ADMIN

Implementar:

Spring Security

JWT

Role Based Access Control

---

# 17. CUSTOMER-SERVICE

Responsável por:

* clientes
* perfil
* endereço
* preferências
* dados agregados
* customer score
* tags
* status do cliente

Dados importantes:

name

email

phone

birthDate

createdAt

lastPurchaseAt

totalSpent

ordersCount

averageTicket

customerScore

segment

---

# 18. CATALOG-SERVICE

Responsável por:

* produtos
* categorias
* imagens
* variações
* atributos
* preço
* promoções
* avaliações

Produto deverá possuir:

ID

SKU

name

description

category

price

promotionalPrice

status

images

variations

createdAt

updatedAt

---

# 19. INVENTORY-SERVICE

Responsável por:

* estoque
* reservas
* movimentações
* estoque disponível
* estoque mínimo

Campos:

available

reserved

physical

minimum

Criar alerta:

LOW_STOCK

Quando estoque disponível ficar abaixo do limite configurado.

---

# 20. ORDER-SERVICE

Responsável pelo ciclo do pedido.

Estados:

CREATED

PAYMENT_PENDING

PAID

PROCESSING

SHIPPED

DELIVERED

CANCELLED

REFUNDED

Guardar:

* items
* quantity
* price
* discount
* shipping
* total
* payment status
* delivery status

---

# 21. PAYMENT-SERVICE

Criar um gateway de pagamento SIMULADO.

Não é necessário utilizar dinheiro real.

Estados:

PENDING

PROCESSING

APPROVED

DECLINED

CANCELLED

REFUNDED

Permitir simular:

* pagamento aprovado
* pagamento negado
* timeout
* erro interno

Isso será utilizado para testar falhas distribuídas.

---

# 22. SAGA PATTERN

Implementar fluxo distribuído.

Fluxo normal:

ORDER_CREATED

↓

INVENTORY_RESERVED

↓

PAYMENT_REQUESTED

↓

PAYMENT_APPROVED

↓

ORDER_CONFIRMED

Se pagamento falhar:

PAYMENT_FAILED

↓

INVENTORY_RELEASED

↓

ORDER_CANCELLED

Documentar claramente a estratégia utilizada.

---

# 23. CRM-SERVICE

Responsável por:

* pipelines
* leads
* oportunidades
* tarefas
* notas
* tags
* interações
* follow-ups

Entidades:

Lead

Opportunity

Pipeline

PipelineStage

Interaction

Task

Note

Tag

---

# 24. ANALYTICS-SERVICE

Tecnologia:

Python + FastAPI

Responsável por:

* RFM
* Customer Score avançado
* segmentação
* churn
* recomendações
* métricas

Comunicação com outros serviços deverá ser documentada.

---

# 25. NOTIFICATION-SERVICE

Responsável por notificações.

Inicialmente:

* notificações internas
* e-mail simulado

Posteriormente:

* WebSocket
* integrações externas opcionais

Eventos:

ORDER_CONFIRMED

PAYMENT_FAILED

LOW_STOCK

CUSTOMER_BECAME_VIP

ABANDONED_CART

CHURN_RISK_DETECTED

---

# 26. KAFKA

Utilizar Apache Kafka para eventos assíncronos.

Eventos sugeridos:

USER_CREATED

CUSTOMER_CREATED

PRODUCT_VIEWED

PRODUCT_ADDED_TO_CART

CART_ABANDONED

ORDER_CREATED

ORDER_CONFIRMED

ORDER_CANCELLED

PAYMENT_REQUESTED

PAYMENT_APPROVED

PAYMENT_FAILED

INVENTORY_RESERVED

INVENTORY_RELEASED

LOW_STOCK

CUSTOMER_SCORE_UPDATED

CUSTOMER_SEGMENT_CHANGED

CUSTOMER_BECAME_VIP

CHURN_RISK_DETECTED

Criar contratos claros para os eventos.

Adicionar:

eventId

eventType

eventVersion

timestamp

correlationId

payload

---

# 27. IDEMPOTÊNCIA

Consumers Kafka devem suportar processamento idempotente.

Um mesmo evento não poderá:

* duplicar pedido
* duplicar pagamento
* duplicar movimentação de estoque
* duplicar oportunidade

Documentar a estratégia.

---

# 28. TRANSACTIONAL OUTBOX

Sempre que apropriado, utilizar o padrão:

Transactional Outbox

Objetivo:

Evitar inconsistências entre banco de dados e publicação Kafka.

Criar documentação explicando o problema resolvido.

---

# 29. REDIS

Utilizar Redis para:

* cache de produtos
* sessões quando necessário
* dados frequentemente acessados
* rate limiting quando apropriado

Não utilizar Redis sem justificativa.

---

# 30. BANCO DE DADOS

Preferência:

PostgreSQL

Cada microsserviço deverá ser dono dos próprios dados.

Evitar que um serviço acesse diretamente tabelas de outro serviço.

Exemplo:

auth_db

customer_db

catalog_db

inventory_db

order_db

payment_db

crm_db

---

# 31. FRONTEND

Tecnologias:

React

TypeScript

Tailwind CSS

TanStack Query

Zustand

React Hook Form

Zod

Recharts

Utilizar arquitetura frontend organizada.

---

# 32. DESIGN

Quero uma interface com aparência profissional de SaaS.

Evitar:

* excesso de gradientes
* cards flutuantes sem função
* aparência genérica de projeto gerado por IA
* animações exageradas
* glassmorphism em excesso

Priorizar:

* hierarquia visual
* espaçamento
* tipografia
* tabelas profissionais
* dashboards
* sidebar
* command palette quando apropriado
* estados vazios
* skeleton loading
* feedback de ações

---

# 33. PÁGINAS DO E-COMMERCE

Criar:

/

/products

/products/:slug

/categories/:slug

/cart

/checkout

/login

/register

/account

/account/orders

/account/orders/:id

/favorites

---

# 34. PÁGINAS ADMINISTRATIVAS

Criar:

/admin

/admin/dashboard

/admin/customers

/admin/customers/:id

/admin/crm

/admin/crm/pipeline

/admin/orders

/admin/orders/:id

/admin/products

/admin/products/new

/admin/products/:id

/admin/inventory

/admin/coupons

/admin/analytics

/admin/notifications

/admin/audit

/admin/settings

---

# 35. CUSTOMER DETAIL

Esta deverá ser uma das telas visualmente mais completas.

Header:

Nome

Avatar

Segmento

Customer Score

Tags

Depois:

Overview

Orders

Timeline

CRM

Notes

Tasks

Analytics

Métricas:

Total spent

Orders

Average ticket

Last purchase

Frequency

Churn risk

---

# 36. CRM KANBAN

Criar drag and drop.

Colunas:

New Lead

Contacted

Qualified

Negotiation

Customer

Lost

Cada card deve exibir:

nome

empresa quando aplicável

valor potencial

responsável

última atividade

próximo follow-up

tags

---

# 37. AUDIT LOG

Criar registro de ações administrativas.

Exemplo:

08/09/2026 16:42

[admin@commerceflow.com](mailto:admin@commerceflow.com)

UPDATED_PRODUCT_PRICE

Bota Western

R$ 599 → R$ 649

Registrar:

actor

action

resource

resourceId

timestamp

before

after

IP quando apropriado

---

# 38. OBSERVABILIDADE

Adicionar em uma fase avançada:

OpenTelemetry

Prometheus

Grafana

Distributed Tracing

Health Checks

Structured Logging

Quero conseguir acompanhar:

Frontend

↓

Gateway

↓

Order Service

↓

Kafka

↓

Payment Service

↓

Inventory Service

Criar correlationId para rastrear requisições distribuídas.

---

# 39. DOCKER

Toda a aplicação deverá funcionar localmente utilizando Docker.

Criar:

Dockerfile para cada aplicação.

docker-compose.yml

Containers:

frontend

gateway

auth-service

customer-service

catalog-service

inventory-service

order-service

payment-service

crm-service

analytics-service

notification-service

postgres

redis

kafka

Serviços adicionais de observabilidade podem ficar em um profile separado.

---

# 40. CI/CD

Utilizar GitHub Actions.

Pipeline mínimo:

Install

↓

Lint

↓

Build

↓

Unit Tests

↓

Integration Tests

↓

Docker Build

Adicionar workflows independentes quando fizer sentido.

---

# 41. TESTES JAVA

Utilizar:

JUnit

Mockito

Spring Boot Test

Testcontainers

Criar:

unit tests

integration tests

repository tests

controller tests

Não criar testes apenas para aumentar coverage.

Testar regras importantes.

---

# 42. TESTES FRONTEND

Criar testes das funcionalidades mais importantes.

Priorizar:

login

carrinho

checkout

CRM

permissões

formulários críticos

---

# 43. TESTES PYTHON

Utilizar pytest.

Testar:

RFM

Customer Score

segmentação

recomendações

APIs

---

# 44. API DOCUMENTATION

Utilizar OpenAPI / Swagger.

Cada serviço deverá documentar seus endpoints.

Criar exemplos de:

requests

responses

errors

status codes

---

# 45. PADRÃO DE ERROS

Criar estrutura consistente.

Exemplo:

{
"timestamp": "...",
"status": 404,
"code": "CUSTOMER_NOT_FOUND",
"message": "Customer not found",
"path": "/customers/123",
"correlationId": "..."
}

---

# 46. SEGURANÇA

Implementar:

JWT

Refresh Token

RBAC

Password hashing

Validation

CORS

Rate Limiting

Input sanitization quando necessário

Proteção de endpoints administrativos

Nunca colocar:

senhas

API keys

tokens

secrets

no código ou GitHub.

Utilizar variáveis de ambiente.

---

# 47. LGPD / PRIVACIDADE

Como o projeto armazena dados de clientes, implementar conceitos básicos de privacidade.

Criar possibilidade de:

* exportar dados
* anonimizar cliente
* excluir conta quando permitido
* controlar dados sensíveis

Não armazenar informações desnecessárias.

Documentar que o projeto é demonstrativo.

---

# 48. ESTRUTURA DO REPOSITÓRIO

Preferência por MONOREPO.

commerceflow/

apps/

web/

services/

gateway/

auth-service/

customer-service/

catalog-service/

inventory-service/

order-service/

payment-service/

crm-service/

notification-service/

analytics-service/

infra/

docker/

kafka/

postgres/

observability/

docs/

architecture/

api/

database/

events/

decisions/

.github/

workflows/

docker-compose.yml

README.md

---

# 49. DOCUMENTAÇÃO DE ARQUITETURA

Criar:

docs/architecture/

Adicionar:

system-overview.md

services.md

event-flow.md

database.md

security.md

observability.md

Criar diagramas usando Mermaid quando possível.

---

# 50. ADR — ARCHITECTURE DECISION RECORDS

Quero documentar decisões importantes.

Exemplo:

ADR-001 — Why PostgreSQL

ADR-002 — Why Kafka

ADR-003 — Microservices boundaries

ADR-004 — Transactional Outbox

ADR-005 — Saga strategy

ADR-006 — Redis caching strategy

Isso deverá mostrar maturidade arquitetural no GitHub.

---

# 51. README PRINCIPAL

Criar um README extremamente profissional.

Estrutura:

# CommerceFlow

Descrição

Demo

Screenshots

Architecture

Features

Tech Stack

Services

Event Architecture

Database

Running Locally

Environment Variables

Testing

Observability

API Documentation

Roadmap

Architecture Decisions

Contributing

License

Adicionar badges de:

build

tests

Java

Python

React

Docker

---

# 52. SEED

Criar dados fictícios.

Exemplo:

500 clientes

100 produtos

10 categorias

1.500 pedidos

oportunidades CRM

interações

carrinhos abandonados

clientes VIP

clientes em risco

Isso permitirá que o dashboard pareça um sistema real.

Não utilizar dados pessoais reais.

---

# 53. USUÁRIOS DEMONSTRATIVOS

Criar contas seed.

ADMIN

[admin@commerceflow.demo](mailto:admin@commerceflow.demo)

MANAGER

[manager@commerceflow.demo](mailto:manager@commerceflow.demo)

CUSTOMER

[customer@commerceflow.demo](mailto:customer@commerceflow.demo)

As senhas deverão existir apenas em ambiente demo e estar documentadas adequadamente.

---

# 54. ROADMAP

Não tente desenvolver todo o projeto de uma vez.

Desenvolva seguindo obrigatoriamente estas fases.

---

# FASE 0 — ARQUITETURA

Antes de escrever código:

1. analisar requisitos
2. desenhar arquitetura
3. definir serviços
4. definir entidades
5. definir eventos
6. definir APIs
7. definir banco
8. criar ADRs iniciais
9. criar estrutura do repositório

Somente depois iniciar implementação.

---

# FASE 1 — FOUNDATION

Criar:

monorepo

Docker

PostgreSQL

Gateway

Auth Service

Frontend

Login

Cadastro

JWT

Roles

CI inicial

Resultado esperado:

Usuário consegue criar conta e acessar aplicação.

---

# FASE 2 — CATALOG

Implementar:

produtos

categorias

variações

imagens

busca

filtros

página de produto

Resultado:

E-commerce navegável.

---

# FASE 3 — CART

Implementar:

carrinho

favoritos

cupons

cálculo de preço

checkout inicial

---

# FASE 4 — ORDERS

Implementar:

Order Service

Inventory Service

estoque

reserva

pedidos

status

histórico

---

# FASE 5 — PAYMENTS

Implementar:

Payment Service

pagamento simulado

falhas simuladas

Saga

compensação de estoque

---

# FASE 6 — KAFKA

Transformar processos apropriados em event-driven.

Adicionar:

Kafka

event schemas

correlationId

idempotência

Transactional Outbox

---

# FASE 7 — CRM

Implementar:

CRM Service

pipeline

leads

opportunities

notes

tasks

interactions

Kanban

---

# FASE 8 — CUSTOMER 360

Implementar:

Customer Profile

timeline

tags

total spent

average ticket

last purchase

orders count

engagement

Customer Score

---

# FASE 9 — AUTOMAÇÕES

Criar regras:

VIP

recorrente

carrinho abandonado

cliente em risco

estoque baixo

criação automática de oportunidades.

---

# FASE 10 — ANALYTICS PYTHON

Criar:

analytics-service

FastAPI

RFM

segmentação

customer score

recomendações

dashboard analytics

---

# FASE 11 — CHURN

Implementar análise de churn.

Não utilizar dados falsos para alegar precisão real.

Tratar como funcionalidade experimental/demo.

---

# FASE 12 — NOTIFICATIONS

Criar:

Notification Service

notificações internas

WebSocket

eventos em tempo real

---

# FASE 13 — OBSERVABILITY

Adicionar:

OpenTelemetry

Prometheus

Grafana

Distributed Tracing

Structured Logs

Health Checks

---

# FASE 14 — QUALITY

Revisar:

testes

segurança

performance

erros

edge cases

queries

índices

cache

concorrência

---

# FASE 15 — PORTFOLIO

Finalizar:

README

screenshots

GIFs

diagramas

Swagger

documentação

seed

demo

CI/CD

arquitetura

ADRs

---

# 55. GIT WORKFLOW

Quero que o histórico do GitHub pareça desenvolvimento real.

Não criar um único commit contendo todo o projeto.

Utilizar branches como:

feature/authentication

feature/product-catalog

feature/shopping-cart

feature/order-service

feature/payment-service

feature/crm-pipeline

feature/customer-360

feature/kafka-events

feature/customer-analytics

feature/observability

fix/inventory-concurrency

refactor/order-domain

---

# 56. PADRÃO DE COMMITS

Utilizar Conventional Commits.

Exemplos:

feat(auth): implement JWT authentication

feat(catalog): add product variations

feat(crm): implement opportunity pipeline

feat(events): publish order created event

feat(analytics): add RFM customer segmentation

fix(inventory): prevent duplicate stock reservation

test(order): add checkout integration tests

docs(architecture): document saga flow

refactor(customer): simplify score calculation

---

# 57. PULL REQUESTS

Mesmo sendo um projeto individual, quero utilizar Pull Requests para algumas features importantes.

Cada PR deverá explicar:

What

Why

Architecture impact

Testing

Screenshots quando existir UI.

---

# 58. CÓDIGO

Requisitos:

Código limpo

SOLID quando apropriado

Domain-oriented design

Baixo acoplamento

Alta coesão

DTOs

Validation

Exception handling

Logging

Testability

Não adicionar Design Patterns apenas para parecer sofisticado.

Utilizar padrões somente quando resolvem problemas reais.

---

# 59. NÃO FAZER

Não:

* gerar todo o projeto de uma vez
* criar dezenas de microsserviços sem necessidade
* criar arquivos vazios
* usar TODO como substituto de implementação
* criar mocks e fingir que funcionalidades existem
* deixar endpoints sem autenticação
* colocar secrets no projeto
* copiar código sem padronização
* adicionar dependências sem necessidade
* criar IA onde lógica determinística resolve melhor
* criar README prometendo funcionalidades inexistentes

---

# 60. DEFINIÇÃO DE DONE

Uma feature só pode ser considerada pronta quando:

* funciona
* possui validações
* possui tratamento de erros
* possui testes importantes
* integra corretamente com o restante do sistema
* possui documentação quando necessário
* não quebra CI
* não possui secrets
* não possui TODO crítico
* funciona com Docker

---

# 61. EXPERIÊNCIA DO USUÁRIO

Toda funcionalidade frontend deverá tratar:

Loading

Empty state

Error state

Success state

Permission denied

Mobile layout

Desktop layout

Evitar interfaces que funcionam apenas no cenário perfeito.

---

# 62. PERFORMANCE

Analisar:

N+1 queries

paginação

índices

cache

lazy loading

debounce

queries frontend

payloads excessivos

Não otimizar prematuramente, mas documentar decisões relevantes.

---

# 63. CONCORRÊNCIA DE ESTOQUE

Tratar cenário:

Estoque disponível:

1

Dois clientes tentam comprar simultaneamente.

O sistema não poderá vender duas unidades.

Implementar estratégia adequada de concorrência.

Criar teste específico.

---

# 64. CENÁRIOS DE FALHA

Criar testes para:

Kafka indisponível

pagamento timeout

pagamento recusado

estoque insuficiente

evento duplicado

serviço temporariamente indisponível

pedido duplicado

requisição repetida

token expirado

usuário sem permissão

---

# 65. DEMONSTRAÇÃO FINAL

Quero conseguir demonstrar o sistema seguindo este fluxo:

1. criar cliente
2. visualizar produtos
3. adicionar produto ao carrinho
4. realizar checkout
5. processar pagamento
6. reservar estoque
7. confirmar pedido
8. publicar eventos Kafka
9. atualizar Customer 360
10. atualizar CRM
11. atualizar analytics
12. visualizar timeline
13. visualizar dashboard
14. visualizar logs/traces

Depois demonstrar cenário de erro:

1. criar pedido
2. reservar estoque
3. pagamento falhar
4. Saga executar compensação
5. liberar estoque
6. cancelar pedido
7. registrar eventos
8. gerar notificação

---

# 66. RESULTADO ESPERADO

Ao final, o projeto deverá demonstrar que o desenvolvedor domina:

Java

Spring Boot

Spring Security

React

TypeScript

Python

FastAPI

PostgreSQL

Redis

Kafka

Docker

REST APIs

Microsserviços

Event-driven architecture

Distributed systems

CRM

E-commerce

Customer Analytics

Testing

CI/CD

Observability

Software Architecture

---

# INSTRUÇÃO FINAL PARA A IA

Você será meu **Senior Software Engineer e Software Architect** durante o desenvolvimento deste projeto.

Não quero que simplesmente gere milhares de linhas de código.

Quero desenvolver o CommerceFlow de maneira progressiva e profissional.

Em cada fase:

1. Explique brevemente o objetivo.
2. Analise as decisões arquiteturais necessárias.
3. Mostre quais arquivos serão criados/modificados.
4. Implemente a funcionalidade completamente.
5. Adicione os testes necessários.
6. Execute ou valide os testes.
7. Corrija erros encontrados.
8. Atualize documentação relevante.
9. Sugira os commits da fase.
10. Informe claramente o que foi concluído.

Não avance deixando código quebrado.

Não crie funcionalidades falsas.

Não utilize placeholders como implementação final.

Não reescreva partes funcionando sem necessidade.

Respeite a arquitetura existente.

Sempre verifique o código atual antes de modificar alguma coisa.

Priorize qualidade, legibilidade, segurança e manutenibilidade.

Quando houver mais de uma abordagem possível, escolha a mais adequada para um projeto profissional de portfólio e explique resumidamente a decisão.

IMPORTANTE:

Não comece implementando tudo.

Comece exclusivamente pela:

**FASE 0 — ARQUITETURA**

Primeiro entregue:

1. Arquitetura geral
2. Diagrama
3. Limites dos microsserviços
4. Estrutura do monorepo
5. Entidades principais
6. Banco de dados
7. Eventos Kafka
8. APIs principais
9. Fluxo completo de checkout
10. Fluxo Saga
11. Estratégia de autenticação
12. Estratégia de observabilidade
13. ADRs iniciais
14. Roadmap técnico

Somente depois da minha aprovação comece a FASE 1.

Projeto evoluir visualmente nessa ordem: loja funcional → painel administrativo → CRM Kanban → Customer 360 → Analytics → observabilidade.
