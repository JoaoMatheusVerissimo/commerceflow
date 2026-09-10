# Fase 1 — execução e verificação

## Escopo implementado

Monorepo Maven (Java 21) e React/TypeScript; gateway reativo, Auth, perfil mínimo Customer, bancos independentes, Flyway, cadastro/login, Argon2, RS256/JWKS, refresh rotativo, logout, RBAC, CSRF, CORS e correlation ID. Página de conta consome o perfil real e trata carregamento/erro/sucesso. Não há catálogo, compras ou painel administrativo.

## Docker local

Pré-requisitos: Docker Engine/Desktop em modo Linux com Compose v2. A stack usa aproximadamente três JVMs, dois bancos e nginx; reserve memória suficiente (6 GB é um ponto de partida).

1. Copie `.env.example` para `.env` e defina uma senha local única em `DB_PASSWORD`. Não versione esse arquivo.
2. Na raiz: `docker compose up -d --build`.
3. Acompanhe: `docker compose logs -f auth-service customer-service gateway`.
4. Abra `http://localhost:3000`, cadastre um usuário fictício e consulte a página de conta.
5. Execute `node scripts/smoke-foundation.mjs` com Node 24 para o teste HTTP ponta a ponta. Ele cria uma conta fictícia de teste no banco local.
6. Pare com `docker compose down`. Os volumes são preservados. `docker compose down -v` apaga contas e sessões; use apenas se quiser descartar os dados locais.

Portas vinculadas somente a localhost: frontend 3000, gateway 8080, auth-db 5433, customer-db 5434. Os serviços Java internos não são expostos pelo Compose. A senha só é aplicada na primeira inicialização do volume PostgreSQL; alterar `.env` não altera usuários de volumes existentes.

## Desenvolvimento sem containers para as aplicações

Ainda requer PostgreSQL: inicie `docker compose up -d auth-db customer-db`. Configure `JAVA_HOME` para JDK 21 e exporte `DB_PASSWORD` nos terminais Java (Spring não carrega `.env` automaticamente).

Na raiz, execute `./mvnw -B -ntp verify` (PowerShell: `.\mvnw.cmd -B -ntp verify`). O wrapper usa Maven 3.9.16. Depois, em terminais separados:

```text
java -jar services/customer-service/target/customer-service-0.1.0-SNAPSHOT.jar
java -jar services/auth-service/target/auth-service-0.1.0-SNAPSHOT.jar
java -jar services/gateway/target/gateway-0.1.0-SNAPSHOT.jar
```

Em `apps/frontend`, execute `npm ci` e `npm run dev`; abra `http://localhost:5173`. Vite encaminha `/api` ao gateway. Para smoke via Vite, configure `SMOKE_BASE_URL=http://localhost:5173`.

## Variáveis

| Variável | Uso |
| --- | --- |
| DB_PASSWORD | Obrigatória no Compose; não há senha de aplicação versionada |
| DB_URL / DB_USERNAME | Conexão do banco exclusivo de cada serviço |
| JWT_ISSUER | Mesmo issuer em Auth, Gateway e Customer |
| JWT_PRIVATE_KEY_FILE | Arquivo privado RSA PKCS#8 PEM externo ao Git; opcional apenas em demo |
| SECURE_COOKIES | `false` somente para HTTP local; `true` em HTTPS |
| CUSTOMER_SERVICE_URL / AUTH_SERVICE_URL | Endereços internos HTTP |
| JWK_SET_URI | URL interna do JWKS no gateway e no Customer |
| CORS_ALLOWED_ORIGINS | Lista explícita de origens permitidas |
| AUTH_RATE_LIMIT_REQUESTS / AUTH_RATE_LIMIT_WINDOW | Padrão: 10 tentativas por minuto, por IP/endpoint |

Para chave persistente, gere externamente com `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out auth-private.pem`, mantenha permissões restritas e informe o caminho em `JWT_PRIVATE_KEY_FILE`. No Docker, monte o arquivo read-only via override local e configure o caminho interno. Não copie a chave para a imagem. Produção/gestão automática de rotação de chaves não fazem parte desta entrega.

## Testes e CI

```text
./mvnw -B -ntp verify
cd apps/frontend
npm ci
npm run lint
npm test
npm run build
npm audit --audit-level=high
```

Maven executa Enforcer, Checkstyle, JUnit/Mockito, testes HTTP e persistência. Dois testes de migrations PostgreSQL são pulados quando Docker está indisponível; isso não equivale a validar Docker. Testes de transação Auth usam H2 com migrations reais e mock somente da fronteira HTTP Customer.

`.github/workflows/foundation.yml` executa Java, frontend e Compose com smoke real. A CI não publica imagens, não faz deploy e não cria usuários administrativos. Usa as actions oficiais [checkout](https://github.com/actions/checkout), [setup-java](https://github.com/actions/setup-java) e [setup-node](https://github.com/actions/setup-node).

## APIs reais

Prefixo público: `/api/v1`. Auth: `POST /auth/register`, `/login`, `/refresh`, `/logout`, `/logout-all`; `GET /auth/me`, `/auth/.well-known/jwks.json`. Customer: `GET /customers/me`. Endpoint exclusivamente interno: `PUT /internal/customers/{userId}`.

```json
{"name":"Ana Demo","email":"ana@example.test","password":"ExamplePassword123"}
```

Cadastro e login retornam 200 com `{accessToken, tokenType, expiresIn}` e cookies de sessão. Login recebe apenas email/password. Refresh e logout usam cookies + `X-CSRF-Token`; `/me` e logout-all usam `Authorization: Bearer <accessToken>`. Nenhum token deve ser versionado ou gravado em localStorage.

Erros: 400 validação; 401 autenticação/refresh inválido; 403 autorização; 409 cadastro não concluído; 429 rate limit; 503 provisionamento indisponível. Envelope conforme `contracts/http/error-response.schema.json`, com `fieldErrors` em validações.

OpenAPI é gerado pelos endpoints reais: `http://localhost:8081/v3/api-docs` e `http://localhost:8082/v3/api-docs` no modo de desenvolvimento Java; UI em `/swagger-ui/index.html`. No Compose, essas portas não são publicadas: acesse pela rede interna ou por override local. Não confundir o catálogo arquitetural planejado com endpoints disponíveis hoje.

## Pendências de aceite

Verificação em 2026-09-09: Maven `verify` passou (13 testes executados com sucesso; 2 testes PostgreSQL pulados localmente), Checkstyle sem violações. Frontend: 6 testes passaram; ESLint e build passaram. Auditoria npm: 0 vulnerabilidades na consulta realizada. Corrigida limpeza de DOM entre testes React; caches TypeScript movidos para diretório ignorado.

Na [execução GitHub Actions do commit b494875](https://github.com/JoaoMatheusVerissimo/commerceflow/actions/runs/34429464238), job Java e verificação de disponibilidade Docker passaram; frontend falhou em `npm test`, impedindo Compose. Os ajustes finais ainda precisam ser commitados/enviados pelo usuário para validar uma nova execução.

Ambiente desta implementação sem Docker: PostgreSQL/Testcontainers e smoke Compose não foram executados localmente. A Fase 1 só deve ser marcada concluída após CI/Compose verdes. Não avançar à Fase 2. Limitações de produção e integração temporária estão no ADR-010.
