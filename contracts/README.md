# Contracts

Contratos compartilhados são artefatos de integração, não bibliotecas de domínio compartilhadas.

- `events/`: envelope e, futuramente, schemas de payload compatíveis com o catálogo de eventos.
- `http/`: formatos transversais de protocolo, como a resposta padronizada de erro.

Cada serviço continua dono de seu modelo interno. Alterações incompatíveis exigem nova versão de contrato.
