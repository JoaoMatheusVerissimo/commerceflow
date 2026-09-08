# ADR-007 — JWT curto e refresh token rotativo

- Status: Aceito
- Data: 2026-09-08

## Contexto

Frontend, gateway e múltiplos serviços precisam autenticar clientes e equipe sem consultar Auth em toda requisição. Ao mesmo tempo, tokens longos no navegador aumentam impacto de roubo.

## Decisão

Auth emite JWT de acesso curto com assinatura assimétrica e publica JWKS. Refresh tokens são opacos, armazenados apenas como hash, enviados em cookie seguro e rotacionados a cada uso com detecção de reuse.

Gateway e serviço de destino validam access token. RBAC é complementado por ownership e regras do recurso.

## Consequências

- validação distribuída não depende de chamada síncrona a Auth;
- rotação de chaves, audiences e clock skew precisam ser testados;
- revogação imediata de JWT já emitido é limitada por sua curta expiração;
- fluxo de cookie exige defesa CSRF e configuração CORS precisa;
- frontend não persiste bearer token em `localStorage`.

## Alternativas consideradas

- Sessão central em todas as requisições: rejeitada pelo acoplamento e ponto central de latência.
- Refresh token no armazenamento JavaScript: rejeitado pelo maior risco de exfiltração via XSS.
