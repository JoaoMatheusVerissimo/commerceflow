# ADR-010 — Integração mínima da Foundation

- Status: Aceito para a implementação da Fase 1 autorizada pelo usuário
- Data: 2026-09-09

## Decisão

Manter Auth e Customer separados, cada um com PostgreSQL e migrations próprias. Até a Fase 6, Auth provisiona o perfil por HTTP com JWT de serviço de 60 segundos, audience `customer-service` e scope `customer:provision`. O gateway não publica rotas internas. O serviço valida JWT novamente.

Cadastro persiste primeiro uma identidade `PENDING_PROFILE`, com nome temporário. Só emite sessão depois de provisionar Customer e ativar a identidade, apagando o nome temporário no Auth. Uma falha de rede retorna 503; repetir cadastro com as mesmas credenciais ou fazer login retoma o provisionamento. O PUT é idempotente por `userId`, protegido por índice único. Chamadas concorrentes podem exigir nova tentativa após conflito; não se promete transação distribuída.

Não há Kafka, Outbox, catálogo nem CRM nesta fase. A migração para eventos continua prevista na Fase 6, conforme ADR-002/004. Auth não consulta tabelas de Customer.

## Segurança e sessão

- Access JWT RS256, 15 minutos, issuer e audience validados; refresh opaco de 30 dias, somente hash no banco.
- Rotação transacional com bloqueio pessimista; reuse com CSRF válido revoga a família, inclusive quando a operação termina em erro.
- Refresh HttpOnly restrito a `/api/v1/auth`; cookie CSRF legível no caminho `/`, vinculado por hash à sessão; SameSite=Lax. HTTP local usa `Secure=false`; HTTPS exige `Secure=true`.
- Logout valida refresh + CSRF e não exige access token ainda válido. Logout global exige bearer e revoga sessões, mas access JWTs já emitidos continuam válidos até expirar.
- Frontend mantém bearer em memória, compartilha renovações simultâneas no mesmo contexto e renova antes da expiração. Abas distintas ainda podem disputar refresh; reuse encerra a família por segurança.
- Roles CUSTOMER, SELLER, MANAGER e ADMIN são modeladas. Cadastro público concede apenas CUSTOMER. Não há interface administrativa nem endpoint de promoção de roles nesta fase.
- `JWT_PRIVATE_KEY_FILE` aceita chave RSA privada PKCS#8 PEM externa. Sem arquivo, gera chave efêmera para demonstração local; reiniciar Auth invalida access tokens antigos. Não usar esse modo em produção ou múltiplas réplicas.

## Limitações explícitas

O rate limit é local ao gateway, por IP remoto e endpoint; no Compose, nginx compartilha o IP percebido. Não confiamos em cabeçalhos de IP enviados pelo cliente. Um proxy confiável e limite distribuído são necessários antes de exposição pública.

Login usa erro genérico e hashing também para identidade inexistente. Cadastro síncrono ainda diferencia sucesso de rejeição (409): resistência completa à enumeração requer verificação de e-mail/resposta assíncrona, ainda ausente. Não alegamos que a Foundation esteja pronta para produção.

Testes H2 verificam fluxo JPA/transações, mas não substituem PostgreSQL. Testcontainers verifica migrations e a CI executa smoke HTTP sobre o Compose real. A Definition of Done permanece pendente até essa execução passar.
