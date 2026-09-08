# Estratégia de autenticação e segurança

## Identidade

Auth Service é a autoridade de credenciais. O registro cria `UserAccount` e publica `UserCreated`; Customer Service cria o perfil comercial de forma idempotente e publica `CustomerCreated`.

Senhas usam hash forte, salt individual e parâmetros configuráveis (Argon2id como baseline). Senha em texto puro existe apenas durante a requisição e nunca entra em log, evento ou auditoria.

## Sessão

- access token JWT assinado assimetricamente, vida curta (baseline de 15 minutos);
- claims mínimas: `sub`, `roles`, `permissions`, `iat`, `exp`, `iss`, `aud`, `jti`;
- chave identificada por `kid`, rotação e publicação por JWKS;
- refresh token opaco, aleatório, armazenado no banco somente como hash;
- refresh token com rotação a cada uso, família de sessão e detecção de reuse;
- baseline de 30 dias para refresh, configurável por ambiente;
- revogação de sessão no logout, troca de senha ou evento de segurança.

No navegador, access token fica somente em memória. Refresh token usa cookie `HttpOnly`, `Secure`, `SameSite=Lax` e escopo de path restrito. A operação de refresh recebe proteção CSRF compatível com cookie. Tokens não usam `localStorage`.

## Validação distribuída

Gateway valida assinatura, issuer, audience, expiração e aplica regras grosseiras de rota. Cada serviço valida o JWT novamente e aplica autorização de domínio. Assim, acesso direto indevido a um serviço não contorna segurança.

O gateway propaga o bearer token original e headers de correlação; não cria headers de identidade confiando no cliente. Chamadas service-to-service administrativas usarão identidade de workload separada do usuário quando forem introduzidas.

## Autorização

| Role | Escopo geral |
|---|---|
| `CUSTOMER` | recursos próprios da loja e conta |
| `SELLER` | clientes e operações CRM atribuídas, leitura operacional de pedidos |
| `MANAGER` | gestão comercial, catálogo, estoque, reembolsos e analytics |
| `ADMIN` | usuários, roles, configurações de plataforma e simuladores demo |

RBAC define capacidade ampla; ownership e estado do recurso completam a decisão. Exemplo: `CUSTOMER` só lê o próprio pedido, e `SELLER` não altera roles.

## Controles transversais

- validação estrutural e de domínio em toda entrada;
- prepared statements/ORM sem concatenação de SQL;
- CORS com allowlist por ambiente;
- rate limit mais rígido em login, registro e refresh;
- respostas de autenticação não revelam se um e-mail existe;
- headers de segurança e CSP no frontend;
- secrets fornecidos por ambiente/secret manager, nunca pelo Git;
- auditoria de login, mudança de role, preço, estoque, pedido e reembolso;
- dados sensíveis removidos de logs e eventos por allowlist de campos;
- dependências e imagens verificadas na CI quando cada stack for criada.

## Privacidade

Consentimento de comunicação é separado de termos obrigatórios. Exportação e anonimização são workflows autenticados, reautenticados quando necessário e auditados. Analytics recebe identificadores pseudônimos e somente features necessárias.

## Ameaças prioritárias

- credential stuffing e brute force;
- roubo/reuse de refresh token;
- IDOR em pedidos, clientes e CRM;
- mass assignment em APIs administrativas;
- manipulação de preço/cupom pelo cliente;
- replay de checkout/pagamento;
- exposição de PII em logs, Kafka ou traces;
- elevação de privilégio por role forjada.

Testes de segurança dessas ameaças entram nas fases que implementarem os respectivos fluxos.
