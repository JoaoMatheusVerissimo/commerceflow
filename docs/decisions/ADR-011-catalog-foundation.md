# ADR-011 — Catálogo navegável e gestão mínima

- Status: Aceito na Fase 2 autorizada pelo usuário
- Data: 2026-09-13

## Contexto e limites preservados

Catalog é dono de produto, categoria, variante/SKU, imagem, preço e avaliação. Não possui saldo de estoque, carrinho ou pedido. A implementação mantém PostgreSQL por serviço, JWT emitido por Auth e validação também no destino. Kafka/Outbox continuam previstos para a Fase 6.

## Decisões

1. **Agregado Product:** categoria, slug único, descrição, estado e listas ordenadas de variantes/imagens. Cada variante tem SKU globalmente único, cor, tamanho e preço. A combinação cor/tamanho é única dentro do produto. Valores monetários são `BigDecimal`/`NUMERIC(12,2)`, enviados como strings decimais com moeda BRL. Promoção deve ser positiva e menor que o preço-base. O menor preço efetivo é recalculado e persistido na mesma transação do produto.
2. **Publicação:** somente produtos ACTIVE são públicos. DRAFT e ARCHIVED permanecem acessíveis à gestão. Arquivar substitui exclusão física nesta fase, preservando identidade e avaliações. PUT substitui a representação editável completa; `version` é obrigatória nas atualizações por regra de negócio. JPA usa versão otimista e o servidor retorna 409 para edição obsoleta ou conflito de unicidade.
3. **Busca:** nome/descrição sem distinção de maiúsculas, com parâmetros e escape de curingas SQL. Filtros por categoria e menor preço efetivo do produto, ordenação por preço/nome/data com desempate por ID. Página de 1 a 48 itens; índice de página de 0 a 10000. Não há busca fonética, normalização de acentos ou promessa de escala de mecanismo especializado.
4. **Consultas:** categoria carregada por EntityGraph; coleções por batch de até 50. O teste de regressão limita a listagem a quatro comandos SQL, evitando uma consulta por produto. Índices cobrem slug, SKU e status/categoria/preço. Redis não foi adicionado: ADR-006 permite início sem cache; ainda não há medição de carga que justifique sua introdução. O teste mede contagem de queries, não latência ou capacidade de produção.
5. **Avaliações:** uma por usuário/produto; somente CUSTOMER escreve. Entram PENDING e são publicadas após aprovação por MANAGER/ADMIN. A API pública não expõe o ID do autor. Não se alega compra verificada: pedidos não existem nesta fase. Edição pelo autor, denúncias e antifraude ficam para evolução posterior.
6. **Administração:** tela mínima de produtos, categorias e moderação, protegida pelo backend. Não é o painel executivo/CRM. Alterações registram ator, ação, recurso e instante na tabela local `catalog_audit`, na mesma transação. O histórico não contém snapshots completos before/after; a auditoria consolidada por eventos permanece futura.
7. **Imagens e seed:** referências HTTPS ou SVGs ilustrativos locais; nenhum download server-side nem upload de arquivos. Seis produtos, três categorias, duas variantes e duas imagens por produto no seed demonstrativo. Seed só é habilitado explicitamente e não sobrescreve um catálogo já iniciado. Não há avaliações fabricadas nem contas administrativas com senha fixa.
8. **Integração:** gateway expõe GET público de produtos/categorias, enquanto escritas exigem JWT e as roles do serviço. Cadastro, login, refresh e perfil da Foundation são preservados. A CI executa os dois smokes sobre Compose, além dos testes Java e React.

## Consequências e limites

- A paginação é estável para dados inalterados; alterações concorrentes podem deslocar páginas.
- Ordenação/filtro de preço usam o valor “a partir de”, não qualquer variante dentro do intervalo.
- Categorias são uma taxonomia plana, com limite operacional de 200 novas categorias verificado pela aplicação, não uma garantia concorrente de quota.
- Imagens HTTPS externas dependem do host e podem expor metadados de navegação; o seed usa apenas arquivos locais. CDN, upload, validação de conteúdo e gestão de mídia não foram implementados.
- Acesso administrativo local exige atribuição explícita de role por operador do banco Auth. Não foi criado endpoint público de promoção de privilégios.
- Retenção de auditoria, rotação de chaves em produção, rate limit distribuído e proteção avançada contra abuso continuam pendentes antes de exposição pública.

## Evidência

O commit `528de2a` passou nos jobs Java, frontend e Compose, incluindo o smoke de catálogo, na [execução 34667831031](https://github.com/JoaoMatheusVerissimo/commerceflow/actions/runs/34667831031). A evidência foi consultada em 2026-09-13. Isso valida a fatia demonstrativa da Fase 2, não uma implantação de produção.
