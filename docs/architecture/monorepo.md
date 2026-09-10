# Estrutura do monorepo

## Estrutura alvo

Na Fase 1, a implementação usa `apps/frontend` (equivalente ao `apps/web` planejado) e `compose.yml` na raiz. Dockerfiles ficam junto de cada aplicação. A árvore abaixo representa o alvo completo, não serviços já implementados.

```text
commerceflow/
├── apps/
│   └── web/                         # React + TypeScript
├── services/
│   ├── gateway/                     # Spring Cloud Gateway
│   ├── auth-service/                # Spring Boot
│   ├── customer-service/            # Spring Boot
│   ├── catalog-service/             # Spring Boot
│   ├── inventory-service/           # Spring Boot
│   ├── order-service/               # Spring Boot
│   ├── payment-service/             # Spring Boot
│   ├── crm-service/                 # Spring Boot
│   ├── notification-service/        # Spring Boot
│   └── analytics-service/           # FastAPI
├── contracts/
│   ├── events/                      # AsyncAPI e JSON Schemas
│   └── http/                        # schemas transversais de protocolo
├── infra/
│   ├── docker/                      # imagens e configuração local
│   ├── kafka/                       # tópicos e configuração
│   ├── postgres/                    # bootstrap de databases/usuários
│   └── observability/               # OTel, Prometheus e Grafana
├── docs/
│   ├── api/
│   ├── architecture/
│   ├── database/
│   ├── decisions/
│   └── events/
├── .github/
│   └── workflows/
├── docker-compose.yml
└── README.md
```

## Convenções

- Cada serviço Java possui build, migrations, Dockerfile e testes próprios; não há entidade JPA compartilhada.
- O serviço Python possui ambiente, migrations, testes e imagem próprios.
- O frontend consome clientes gerados ou tipados a partir de OpenAPI quando isso reduzir divergência.
- `contracts` contém somente artefatos de integração versionados.
- `infra` não contém secrets; valores locais entram por `.env` ignorado e exemplos seguros.
- workflows detectam caminhos alterados para evitar construir todo o monorepo desnecessariamente.

## Estrutura interna recomendada

Serviços de domínio seguem módulos orientados a capacidade, mantendo domínio independente de framework:

```text
src/main/java/.../
├── <capability>/
│   ├── domain/
│   ├── application/
│   ├── adapter/in/web/
│   └── adapter/out/persistence/
└── shared/                          # apenas infraestrutura transversal local
```

Não será criado um framework interno compartilhado no início. Duplicação pequena de infraestrutura é preferível ao acoplamento binário entre todos os serviços.

## Estratégia incremental

Apenas diretórios com documentação ou implementação real são versionados. Os diretórios executáveis aparecem junto com a fase responsável por entregar uma fatia funcional, evitando árvores de scaffolding vazias.
