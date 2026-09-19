# Fase 4 — Orders e Inventory

## Escopo entregue

A Fase 4 transforma a revisão do carrinho em um pedido persistido e reserva estoque sem overselling. O Order Service continua dono do carrinho e passa a manter o snapshot comercial do pedido; o novo Inventory Service é a única autoridade sobre saldo físico, reservado e disponível. Pagamento, compensação, Kafka e Saga assíncrona permanecem nas fases 5 e 6.

A base das Fases 1–3 foi preservada. A Fase 3 teve aceite remoto na [execução 35297557804](https://github.com/JoaoMatheusVerissimo/commerceflow/actions/runs/35297557804), commit `e3c8d22`. A implementação inicial da Fase 4 foi registrada pelo usuário em `53951d9`; as correções finais incluem compatibilidade PostgreSQL, acesso operacional ao detalhe, rota pública de disponibilidade, smoke e documentação.

## Fluxo implementado

1. O cliente envia `POST /checkout`, `cartVersion` e uma `Idempotency-Key` UUID.
2. Order verifica a versão do carrinho e solicita uma nova cotação autoritativa ao Catalog.
3. Na transação local, grava pedido `CREATED`, itens com nome/SKU/slug/preços imutáveis e a chave do comando.
4. Order solicita a reserva ao Inventory por HTTP interno, encaminhando JWT e `X-Correlation-Id`.
5. Inventory ordena os SKUs, bloqueia seus saldos com `FOR UPDATE`, valida todo o conjunto e só então aumenta `reserved`.
6. Com reserva ativa, Order muda para `PAYMENT_PENDING` e limpa o carrinho. Nenhuma cobrança ocorre.
7. Estoque insuficiente muda o pedido para `CANCELLED` e preserva o carrinho. Indisponibilidade transitória mantém `CREATED`, retorna 503 e permite repetir exatamente a mesma chave sem duplicar pedido.

O lock curto e a atualização condicional `physical - reserved >= quantity` protegem a última unidade. A reserva é idempotente por `orderId` e hash dos itens; o mesmo pedido com payload diferente é rejeitado.

## Execução local

Pré-requisitos: Docker Desktop/Engine com containers Linux e Compose, além de Node 24 para os smokes. Configure `DB_PASSWORD` em `.env` e execute:

```powershell
docker compose up -d --build
docker compose ps
node scripts/smoke-foundation.mjs
node scripts/smoke-catalog.mjs
node scripts/smoke-cart.mjs
node scripts/smoke-orders.mjs
```

Abra `http://localhost:3000`, entre com uma conta fictícia, adicione uma variação com estoque ao carrinho e confirme o pedido. O histórico fica em `/account/orders`; o detalhe informa explicitamente que nenhuma cobrança ocorreu. As páginas operacionais são `/admin/orders` e `/admin/inventory` e exigem roles apropriadas.

O Compose acrescenta `inventory-db` em `127.0.0.1:5437`, volume `inventory-data` e serviço interno 8085. Order continua em 8084/5436 e recebe `INVENTORY_SERVICE_URL`. OpenAPI direto está em `/v3/api-docs` de cada serviço quando iniciado fora do Compose. Não use `docker compose down -v` para uma atualização comum, pois essa opção apaga os volumes.

## APIs reais

Prefixo externo `/api/v1`:

| Método / rota | Acesso | Comportamento |
| --- | --- | --- |
| POST `/checkout` | CUSTOMER | Cria/retoma pedido idempotente e tenta reservar estoque; retorna 202 |
| GET `/orders?page=0&size=20` | CUSTOMER | Histórico do próprio cliente |
| GET `/orders/{id}` | dono ou SELLER+ | Snapshot e estado atual; outro cliente recebe 404 |
| GET `/admin/orders?page=0&size=20` | SELLER+ | Consulta operacional paginada |
| GET `/availability/{sku}` | público | Saldo físico, reservado, disponível, mínimo e alerta |
| GET `/admin/inventory` | SELLER+ | Saldos operacionais de todos os SKUs |
| POST `/admin/inventory/{sku}/movements` | MANAGER/ADMIN | Entrada, saída ou ajuste auditável |
| PATCH `/admin/inventory/{sku}/minimum` | MANAGER/ADMIN | Altera limite de baixo estoque |

Somente interno: `POST /internal/reservations`, com JWT CUSTOMER delegado pelo Order. O Gateway não publica a rota. Movimentos aceitam `IN`, `OUT` ou `ADJUSTMENT`, quantidade não negativa conforme a operação, motivo obrigatório e referência opcional. Toda alteração registra ator, saldo anterior/posterior e instante no banco do Inventory.

Exemplo de checkout:

```http
POST /api/v1/checkout
Authorization: Bearer <token>
Idempotency-Key: 2fd3faca-a3bc-438f-b10e-cad1a6e91717
Content-Type: application/json

{"cartVersion":1}
```

Os valores monetários retornam como strings com duas casas e `currency: BRL`. `shipping` permanece `0.00` como ausência de cálculo nesta fase, não como promessa de frete grátis. Estados efetivamente produzidos: `CREATED`, `PAYMENT_PENDING` e `CANCELLED`. Os estados posteriores estão reservados ao workflow futuro e não são simulados.

## Verificação

Resultados locais em 2026-09-19:

- Java: 60 testes, zero falhas, zero erros e zero pulos; Maven `verify`, Checkstyle e empacotamento dos sete módulos aprovados. H2 e PostgreSQL/Testcontainers cobrem Order e Inventory.
- Concorrência: duas reservas disputando a última unidade resultam em exatamente um sucesso, sem saldo negativo; replay e payload divergente também são testados em PostgreSQL.
- Frontend: 25 testes em 6 arquivos; ESLint e build TypeScript/Vite aprovados.
- Dependências: `npm audit --audit-level=high` retornou 0 vulnerabilidades.
- Compose: 12 containers ativos, cinco bancos saudáveis, imagens reconstruídas e smokes Foundation, Catalog, Cart e Orders aprovados.
- Integração do smoke Orders: snapshot, reserva, replay idempotente, ownership, saldo público e limpeza do carrinho verificados contra os serviços e PostgreSQL reais.

A CI executará a mesma regressão após o próximo commit/push. Até essa execução remota ficar verde, o estado é **implementado e validado localmente, aceite remoto pendente**.

## Débitos delimitados

- expiração/liberação/compensação de reservas e pagamento pertencem à Fase 5;
- Kafka, Outbox/Inbox e Saga assíncrona pertencem à Fase 6; a integração atual Order→Inventory é HTTP síncrona e explicitamente transitória;
- endereço, frete calculado, tributação e consumo definitivo de cupom não foram inventados;
- paginação usa offset; cursor pode ser adotado se volume/medição justificar;
- retenção de comandos idempotentes, reservas e snapshots ainda precisa de política operacional;
- matriz completa de acessibilidade, dispositivos e navegadores continua como validação de qualidade futura.

Decisões: [ADR-013](decisions/ADR-013-orders-and-inventory.md). A Fase 5 não foi iniciada.
