# ADR-012 — Carrinho persistente e cotação autoritativa

- Status: aceito para a Fase 3 autorizada pelo usuário
- Data: 2026-09-16

## Contexto

O roadmap exige carrinho, favoritos, cupons, cálculo de preço e checkout inicial. Preservamos os limites da Fase 0: Order possui carrinho; Catalog possui preço/cupom; Customer possui favoritos. Não antecipamos pedidos, Inventory, pagamentos, Saga ou Kafka.

## Decisões

1. **Order Service começa pelo agregado Cart.** Banco `order_db`, migrations próprias, porta interna 8084. Não criamos um cart-service nem entidades de pedido vazias. JDBC resolve comandos transacionais pequenos sem framework compartilhado entre serviços.
2. **Carrinho autenticado por CUSTOMER.** Um por `sub` do JWT; nenhum ID de dono é aceito do navegador. Visitantes podem explorar produtos, mas entram na conta antes de salvar carrinho/favoritos. Carrinho anônimo e merge de sessões não são prometidos.
3. **Comandos PUT de substituição completa**, com `version` obrigatória e `Idempotency-Key` UUID. Até 50 SKUs distintos, 1–99 unidades por SKU. UPDATE condicional impede sobrescrita concorrente; criação inicial usa PK por usuário. Conflitos retornam 409. Itens, versão e resultado idempotente são gravados na mesma transação.
4. **Idempotência por usuário/chave**, com hash do DTO (incluindo versão e ordem dos itens). Repetição do mesmo DTO retorna o resultado original, não a versão atual; chave com outro DTO retorna 409. Corridas ainda em andamento podem retornar 409; repetir a mesma chave após o commit recupera o resultado. O frontend mantém a chave de uma tentativa de resultado incerto enquanto o componente estiver montado. Ao recarregar a página, deve-se consultar o carrinho antes de repetir uma intenção.
5. **Carrinho não é cotação nem reserva.** Guarda referências e quantidades, não preços fornecidos pelo navegador. Permite remover referências indisponíveis mesmo se Catalog estiver fora do ar. Somente a cotação valida existência/estado ACTIVE, vínculo produto/SKU, promoções e cupom. Um cupom salvo no carrinho pode ser inelegível: nesse caso não se apresenta total válido.
6. **Cotação interna HTTP por identidade delegada.** Order encaminha bearer CUSTOMER validado e correlation ID para `/internal/pricing/quote`; Catalog valida assinatura/issuer/audience/role. Gateway não publica `/internal`. É leitura em nome do cliente, sem privilégio de workload ou acesso cruzado ao banco. Timeout de conexão 3 s e requisição 5 s; indisponibilidade retorna 503, inelegibilidade 422. Não há chamada remota dentro da transação de escrita do carrinho.
7. **Snapshot de leitura consistente:** leitura do carrinho e cotação do Catalog usam REPEATABLE_READ em seus respectivos bancos, sem transação distribuída. A resposta de Order inclui `cartVersion`; a UI não apresenta totais de outra versão. Cotação contém instante e validade indicativa de cinco minutos, sem fixar preço para compra futura. Checkout inicial apenas revisa a cotação atual; não coleta endereço/pagamento nem cria pedido.
8. **Cupons determinísticos:** percentual de 0,01 a 100 ou valor fixo positivo em BRL; mínimo do subtotal; início inclusivo/fim exclusivo UTC; ativação e versão. Uma regra por carrinho sobre preços já promocionais. Percentual arredondado HALF_UP no desconto total, duas casas; desconto limitado ao subtotal. Não há resgate/limite de uso, frete ou tributação calculados nesta fase. Gestão MANAGER/ADMIN com auditoria de metadados local.
9. **Favoritos são referências por UUID** no Customer, sem FK remota. PUT/DELETE idempotentes; consulta paginada de 20 itens; limite de 200 por cliente serializado por lock curto no perfil. Produto removido/arquivado continua removível dos favoritos. A UI consulta a API pública por ID, com erro/retry por cartão; não copia dados mestres para Customer.
10. **Infraestrutura incremental:** reutiliza Java 21/Spring Boot, React, clientes tipados e PostgreSQL. Não adiciona Redis, Kafka ou bibliotecas de estado/formulário sem necessidade demonstrada. Testes reais PostgreSQL complementam H2, e smoke no Compose integra Auth, Customer, Catalog, Order e Gateway.

## Limitações e evolução

- Retenção/expurgo de comandos e carrinhos ainda precisa de política; as chaves persistidas não expiram automaticamente.
- Sem carrinho anônimo, fusão de carrinhos, reserva de cupom, uso único, frete ou pedidos nesta fase.
- Favorites faz até 20 consultas HTTP de produto por página; batch/projeção dependerá de medição.
- A leitura de catálogo pode mudar após a cotação. A criação futura de pedidos deve revalidar preço, cupom e estoque e persistir snapshots próprios.
- A segurança de produção e as limitações de sessão do ADR-010 continuam aplicáveis. Não há certificação de prontidão para produção.

Execução e evidências: [Fase 3](../cart.md).
