# Fase 3 — Cart

## Escopo

Carrinho persistente por cliente autenticado, inclusão/alteração/remoção de SKUs, favoritos, cupons percentuais/fixos, cotação autoritativa e revisão inicial de checkout. Sem pedidos, estoque, frete calculado, cobrança ou pagamento. A Fase 4 não foi iniciada.

Base revisada: commit `f8eb4f9`, com Foundation e Catalog preservados. O trabalho inicial desta fase foi registrado pelo usuário em `fc023ae`; os ajustes finais incluem validações, testes, integração e documentação. O problema local de CRLF do `mvnw` estava resolvido (`w/lf`). Não houve reescrita de autenticação ou catálogo funcional.

## Execução local

Pré-requisitos: Docker Desktop/Engine com containers Linux e Compose, Node 24 para os smokes. Mantenha `.env` com o `DB_PASSWORD` já utilizado pelos volumes existentes.

```powershell
docker compose up -d --build
docker compose ps
node scripts/smoke-foundation.mjs
node scripts/smoke-catalog.mjs
node scripts/smoke-cart.mjs
```

Abra `http://localhost:3000`, cadastre uma conta fictícia, abra um produto e adicione a variação escolhida. Acesse `/cart`, altere quantidades e aplique `DEMO10` (10% com mínimo de R$ 100, até 2030-01-01 UTC). Clique em **Revisar checkout** para conferir os valores. `/favorites` lista os produtos salvos. O seed do cupom segue `CATALOG_DEMO_SEED=true`, já usado no Compose: cria apenas o código ausente, sem sobrescrever edições.

Os scripts criam contas fictícias; use apenas ambiente demonstrativo. Repetições rápidas podem atingir o rate limit da Foundation. Não execute `down -v` para atualizar: essa opção apaga volumes. `docker compose down` preserva os dados.

O Order adiciona PostgreSQL em `127.0.0.1:5436`, volume `order-data`, e serviço interno 8084. Gateway recebe `ORDER_SERVICE_URL`; Order recebe `CATALOG_SERVICE_URL`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWK_SET_URI` e `JWT_ISSUER`. Os quatro bancos são independentes. Novas migrations não alteram as V1 existentes de Auth/Customer/Catalog.

Sem containers para Java: siga [Foundation](foundation.md) e [Catalog](catalog.md), inicie também `docker compose up -d order-db`, configure JDK 21 e `DB_PASSWORD`, execute `./mvnw -B -ntp verify` e inicie `services/order-service/target/order-service-0.1.0-SNAPSHOT.jar`. OpenAPI em `http://localhost:8084/v3/api-docs` e `/swagger-ui/index.html`; no Compose essa porta não é publicada.

## Cupons administrativos

Use uma conta MANAGER/ADMIN provisionada pelo [procedimento local explícito](catalog.md#acesso-administrativo-local), faça novo login e abra `/admin/coupons`. É possível criar, carregar por código, alterar e desativar cupons. Datas da tela são UTC. Versões obsoletas e códigos duplicados retornam 409; recarregue antes de editar. Não há contas privilegiadas ou senhas fixas no seed.

## APIs reais

Prefixo externo `/api/v1`:

| Método / rota | Acesso | Comportamento |
| --- | --- | --- |
| GET `/cart` | CUSTOMER | Carrinho próprio; ausente retorna versão 0 e itens vazios |
| PUT `/cart` | CUSTOMER | Substitui itens/cupom, exige versão e `Idempotency-Key` UUID |
| POST `/cart/quote` | CUSTOMER | Cotação atual do carrinho persistido, sem criar pedido |
| GET `/customers/me/favorites?page=0` | CUSTOMER | Página de 20 referências; máximo 200 por cliente |
| PUT `/customers/me/favorites/{productId}` | CUSTOMER | Adiciona referência uma única vez |
| DELETE `/customers/me/favorites/{productId}` | CUSTOMER | Remove referência, mesmo se produto indisponível |
| GET `/products/by-id/{id}` | Público | Produto ACTIVE, usado na resolução de favoritos |
| GET `/admin/coupons/{code}` | MANAGER/ADMIN | Consulta regra e versão |
| PUT `/admin/coupons` | MANAGER/ADMIN | Cria com versão null; atualiza com versão atual |

Somente interno: POST Catalog `/internal/pricing/quote`, com JWT CUSTOMER delegado por Order. Não há rota no gateway para esse endpoint.

PUT `/cart` (substitua UUID/SKU pelos retornados no catálogo):

```json
{
  "version": 0,
  "items": [{"productId":"10000000-0000-0000-0000-000000000001","sku":"BOTA-SERRA-P","quantity":2}],
  "coupon": "DEMO10"
}
```

Preço, usuário e desconto nunca são recebidos como autoridade do navegador. Use `coupon: null` para remover cupom, `items: []` para esvaziar. Quantidades 1–99; até 50 SKUs distintos; SKU até 64 caracteres. A chave idempotente é nova por intenção, mas deve ser reutilizada com o mesmo corpo ao repetir uma tentativa sem resposta. Não use uma nova chave para repetir cegamente uma inclusão.

Cotação contém `cartVersion`, `items` (nome, slug, preço unitário e total), `subtotal`, `discount`, `total`, `currency: BRL`, `coupon`, `quotedAt` e `expiresAt`. Valores monetários cotados são strings com duas casas. O total cobre somente produtos; não significa frete gratuito. Carrinho vazio ou produto/SKU/cupom inelegível retornam 422; indisponibilidade de Catalog retorna 503. O carrinho permanece editável para recuperar esses casos.

Exemplo de cupom (valores da regra aceitam decimais JSON):

```json
{"code":"DEMO20","kind":"FIXED","amount":"20.00","minimum":"100.00","startsAt":"2026-01-01T00:00:00Z","endsAt":"2030-01-01T00:00:00Z","active":true,"version":null}
```

PERCENT limita o percentual a 100. Fim é exclusivo; cupom inativo, expirado ou abaixo do mínimo não é aplicado. Sem acúmulo de códigos, limites por resgate ou restrições por categoria. Erros seguem o envelope padrão: 400 validação; 401 sessão inválida; 403 role; 404 recurso; 409 versão/unicidade; 422 cotação inelegível; 503 dependência indisponível.

## Verificação

```text
./mvnw -B -ntp verify
cd apps/frontend
npm test
npm run lint
npm run build
npm audit --audit-level=high
```

O workflow **CommerceFlow CI** executa todos os módulos Java e os três smokes no Compose. A auditoria npm mantém falha obrigatória para vulnerabilidades altas e ganhou até três tentativas para indisponibilidade transitória do registry. A execução remota das alterações finais exige commit/push pelo usuário; não é substituída por uma alegação de CI já verde.

Resultados locais em 2026-09-17:

- Java: 44 testes, zero falhas, zero erros e zero pulos; Checkstyle e build aprovados. Inclui H2 e PostgreSQL/Testcontainers para migrations, catálogo, favoritos e carrinho, além de duas atualizações concorrentes do mesmo carrinho (um sucesso e um 409).
- Frontend: 22 testes em 5 arquivos; ESLint e build Vite/TypeScript aprovados. Cobertura crítica de item/SKU, versão, idempotência em falha incerta, conflito, cupom inválido, cotação de outra versão, checkout sem ação fictícia, favoritos e permissão de cupons.
- Dependências: `npm audit --audit-level=high` concluiu com 0 vulnerabilidades na consulta realizada.
- Docker Compose local: imagens construídas e serviços Auth, Customer, Catalog, Order, Gateway, frontend e quatro PostgreSQL iniciados. Smokes Foundation, Catalog e Cart aprovados.
- Inspeção visual desktop no navegador local: home e produto carregaram com navegação, conteúdo e estados coerentes, sem quebra visível. CSS contém breakpoints de 800/420 px; uma matriz visual completa de aparelhos, leitores de tela e navegadores permanece pendente.

**Implementação da Fase 3 concluída e validada localmente.** O aceite remoto permanece pendente da primeira execução verde da CI com o commit final. A Fase 4 não foi iniciada.

## Débitos delimitados

Carrinho anônimo/merge, política de expurgo, batch de favoritos, auditoria administrativa com before/after, auditoria ampla de acessibilidade e limites de produção herdados. Pedido, endereço de entrega, reserva de estoque, pagamento, consumo de cupons e Kafka pertencem às próximas fases, não a funcionalidades simuladas nesta entrega.

Decisões: [ADR-012](decisions/ADR-012-cart-and-pricing.md).
