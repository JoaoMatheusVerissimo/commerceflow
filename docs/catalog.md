# Fase 2 — Catalog

## Resultado e escopo

**Implementada e validada na CI**, com fechamento documental em 2026-09-13. Um visitante pode navegar, buscar, filtrar e abrir produtos do seed. Clientes autenticados podem enviar avaliações; MANAGER/ADMIN podem manter produtos/categorias e moderar avaliações.

Inclui `catalog-service`, PostgreSQL próprio, Flyway, OpenAPI, roteamento no gateway e frontend integrado à identidade da Fase 1. Não inclui carrinho, favoritos, checkout, cupons, saldo/reserva de estoque ou eventos Kafka. A Fase 3 não foi iniciada.

## Executar

Requisitos: Docker Engine/Desktop com containers Linux e Compose v2. Para executar os scripts de smoke fora dos containers, Node 24.

1. Configure `.env` a partir de `.env.example`, com uma senha local única em `DB_PASSWORD`.
2. Na raiz, execute `docker compose up -d --build`.
3. Abra `http://localhost:3000` e explore o catálogo.
4. Execute `node scripts/smoke-foundation.mjs` e `node scripts/smoke-catalog.mjs`.

O Compose adiciona `catalog-service` na porta interna 8083 e `catalog-db` na porta local 5435, com volume `catalog-data`. O gateway continua em 8080 e o frontend em 3000. Portas publicadas ficam vinculadas a localhost. Dockerfiles dos quatro serviços Java incluem todos os POMs necessários ao reactor Maven.

Os smokes criam contas e uma avaliação fictícia; use banco demonstrativo, nunca produção. O smoke de catálogo requer o seed habilitado. O limite de autenticação pode retornar 429 se os scripts forem repetidos muitas vezes em menos de um minuto.

`docker compose down` preserva os dados. **`docker compose down -v` apaga os volumes da stack, incluindo contas e catálogo**; use apenas quando quiser descartar esses dados.

### Java/Vite fora de containers

Siga o [guia Foundation](foundation.md) para Auth, Customer e Gateway e inicie também `docker compose up -d catalog-db`. Exporte `DB_PASSWORD` no terminal Java e configure `CATALOG_DEMO_SEED=true` se desejar o seed. Com JDK 21:

```text
./mvnw -B -ntp verify
java -jar services/catalog-service/target/catalog-service-0.1.0-SNAPSHOT.jar
```

No PowerShell, use `.\mvnw.cmd` no lugar de `./mvnw`. Frontend: `npm ci` e `npm run dev` em `apps/frontend`; abra `http://localhost:5173`.

OpenAPI do catálogo no modo Java: `http://localhost:8083/v3/api-docs` e `http://localhost:8083/swagger-ui/index.html`. No Compose, essa porta não é publicada; use override local explícito caso queira expô-la apenas a localhost.

### Configurações do catálogo

| Variável | Padrão / finalidade |
| --- | --- |
| DB_URL | `jdbc:postgresql://localhost:5435/catalog_db` fora do Compose |
| DB_USERNAME / DB_PASSWORD | Credenciais do banco exclusivo do catálogo |
| SERVER_PORT | 8083 |
| JWK_SET_URI / JWT_ISSUER | Mesma autoridade JWT da Foundation |
| CATALOG_DEMO_SEED | `false` por padrão; `true` no Compose demonstrativo |
| CATALOG_SERVICE_URL | No gateway; `http://localhost:8083` ou endereço interno do Compose |

O seed é transacional e só executa se não existirem produtos nem categorias. Reiniciar o serviço não sobrescreve edições. Contém três categorias e seis produtos ilustrativos, sem avaliações artificiais. Imagens próprias em `apps/frontend/public/catalog-images` não precisam de acesso externo.

## Acesso administrativo local

1. Cadastre uma conta fictícia normalmente pela aplicação.
2. Como operador da stack local, abra o banco Auth:

```text
docker compose exec auth-db psql -U commerceflow -d auth_db
```

3. No psql, substitua o e-mail pelo da conta demo escolhida e execute:

```sql
INSERT INTO user_roles (user_id, role)
SELECT id, 'MANAGER' FROM user_accounts
WHERE email = 'seu-email-demo@example.test' AND status = 'ACTIVE'
ON CONFLICT DO NOTHING;
```

O retorno `INSERT 0 1` indica atribuição; `INSERT 0 0` indica conta não encontrada/inativa ou role já existente. Saia com `\q`, faça logout/login na aplicação para emitir outro JWT e abra `/admin/products`.

Essa é uma operação privilegiada manual e exclusivamente demonstrativa, não uma permissão disponível no cadastro público. Não execute em banco de produção. Nenhum serviço acessa tabelas de outro serviço. A role CUSTOMER continua associada à conta.

## Navegação

| Página | Função |
| --- | --- |
| `/` | Apresentação da coleção e catálogo |
| `/products` | Busca, filtros, ordenação e paginação |
| `/categories/:slug` | Catálogo filtrado por categoria |
| `/products/:slug` | Galeria, variantes, preços e avaliações |
| `/account` e `/app` | Perfil autenticado preservado da Foundation |
| `/admin/products` | Lista administrativa, categorias e moderação |
| `/admin/products/new` | Criação de produto |
| `/admin/products/:id/edit` | Edição completa e arquivamento |

Busca/filtros ficam na URL, permitindo compartilhar a consulta e usar voltar/avançar. A UI trata carregamento, vazio, erro, tentativa novamente, sucesso e falta de permissão. Há layouts responsivos por CSS. A validação automatizada de componentes não substitui uma avaliação de acessibilidade ou uma matriz completa de navegadores/dispositivos.

## APIs implementadas

Prefixo externo `/api/v1`. Todos os preços são strings decimais, com moeda BRL.

| Método / rota | Acesso | Resultado |
| --- | --- | --- |
| GET `/products` | Público | Página de produtos ACTIVE |
| GET `/products/{slug}` | Público | Produto ativo, variantes e imagens |
| GET `/categories` | Público | Categorias ordenadas por nome/ID |
| GET `/categories/{slug}/products` | Público | Página de produtos ativos da categoria |
| GET `/products/{productId}/reviews` | Público | Avaliações APPROVED paginadas |
| POST `/products/{productId}/reviews` | CUSTOMER | Avaliação PENDING, status 201 |
| GET `/admin/products` | MANAGER/ADMIN | Página incluindo rascunhos/arquivados |
| GET `/admin/products/{id}` | MANAGER/ADMIN | Representação editável |
| POST `/admin/products` | MANAGER/ADMIN | Criação, status 201 |
| PUT `/admin/products/{id}` | MANAGER/ADMIN | Substituição completa com `version` |
| POST `/admin/categories` | MANAGER/ADMIN | Criação, status 201 |
| PUT `/admin/categories/{id}` | MANAGER/ADMIN | Atualização com `version` |
| GET `/admin/reviews` | MANAGER/ADMIN | Fila PENDING paginada |
| PATCH `/admin/reviews/{id}` | MANAGER/ADMIN | Aprovação/rejeição com `version` |

GET `/products` aceita `q` (até 100 caracteres), `category` (slug), `minPrice`, `maxPrice`, `sort` (`newest`, `name`, `price-asc`, `price-desc`), `page` (zero-based) e `size` (1–48, padrão 12). Preço usa o menor valor efetivo do produto. Categoria inexistente resulta em página vazia; produto inexistente/inativo resulta em 404.

Formato de paginação: `{items, page, size, totalElements, totalPages}`. Ordenação sempre inclui ID como desempate. A busca não remove acentos. A lista administrativa aceita `q`, `page` e `size`; avaliações aceitam `page`/`size`.

Exemplo de criação/edição (substitua `categoryId` por um ID retornado pela API; `version` somente na atualização):

```json
{
  "categoryId": "00000000-0000-0000-0000-000000000002",
  "name": "Bota Demo",
  "slug": "bota-demo",
  "description": "Produto ilustrativo para demonstração.",
  "status": "DRAFT",
  "variants": [
    {"sku":"BOTA-DEMO-P","color":"Marrom","size":"P","price":"199.90","promotionalPrice":"179.90"}
  ],
  "images": [{"url":"/catalog-images/boot.svg","alt":"Ilustração de bota marrom"}]
}
```

Avaliação: `{"rating":5,"comment":"Meu comentário"}`. Moderação: `{"status":"APPROVED","version":0}`. O servidor deriva o autor do JWT; clientes não informam `authorId`.

Erros seguem `contracts/http/error-response.schema.json`: 400 validação/filtro/preço inválido; 401 token ausente/inválido; 403 role sem permissão; 404 recurso não encontrado/oculto; 409 slug/SKU/avaliação duplicada ou edição concorrente. Nenhuma exclusão física ou cotação de checkout é publicada nesta fase.

## Testes e resultados

Comandos executados durante a implementação:

```text
./mvnw -B -ntp verify
cd apps/frontend
npm test
npm run lint
npm run build
```

- Java local: 19 testes passaram; 8 testes dependentes de Docker foram pulados (2 migrations da Foundation e 6 cenários do catálogo em PostgreSQL). Checkstyle e build passaram.
- Catálogo: 6 cenários no H2 e a mesma suíte configurada para PostgreSQL/Testcontainers. Cobre busca, filtros, paginação, OpenAPI, preço, promoção, URLs, roles, slug/SKU únicos, publicação/arquivamento, versão concorrente, privacidade/moderação de avaliações e limite de quatro comandos SQL na listagem.
- Frontend: 12 testes passaram, incluindo os 6 anteriores. Busca e serialização de filtros, estado vazio/erro/retry, seleção de SKU/preço/imagem, falta de permissão e salvamento com bearer/version foram cobertos. Lint e build passaram.
- [CI do commit 528de2a](https://github.com/JoaoMatheusVerissimo/commerceflow/actions/runs/34667831031): Java, frontend e Compose aprovados. As etapas de instalação, lint/testes/build, auditoria npm, construção dos containers e ambos os smokes passaram. Consulta feita em 2026-09-13.
- O smoke Foundation confirma cadastro, perfil, login, rotação/reuse e logout. O smoke Catalog confirma catálogo público, imagens, filtros, proteção administrativa, integração Auth/Customer e avaliação não publicada antes da moderação.

**Aceite da Fase 2 registrado com base nessa execução verde.** Docker foi validado na CI; não foi executado neste computador. Não houve commit/push automático dos ajustes documentais finais. A Fase 3 depende de nova autorização.

## Débitos técnicos delimitados

Busca textual simples; cache adiado até medição; gestão de mídia por referência, sem upload/CDN; auditoria local de metadados sem snapshots completos; acesso administrativo local por operador; avaliações sem compra verificada/antifraude; auditoria de acessibilidade e testes visuais cross-browser ainda não realizados. Não são recursos de carrinho ou checkout implicitamente implementados.

Decisões e limites detalhados no [ADR-011](decisions/ADR-011-catalog-foundation.md). Limitações herdadas de autenticação/produção permanecem no [ADR-010](decisions/ADR-010-foundation-integration.md).
