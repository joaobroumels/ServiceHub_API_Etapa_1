# Validação da continuação — 26/10/2026

> Atualização em 27/10/2026: antes da publicação no repositório ServiceHub, `mvn verify` foi executado diretamente na pasta do projeto, com o `pom.xml` entregue e sem adaptação do compilador. Resultado: `BUILD SUCCESS`, 31 testes executados, zero falhas, zero erros e zero ignorados. O Maven utilizou Java 22 com compilação `release 21`. O registro abaixo preserva as condições da validação inicial; suas pendências refletem aquele momento.

## Aplicação

A suíte executou **31 testes, zero falhas, zero erros e zero ignorados**:

| Classe | Testes |
|---|---:|
| AuthControllerWebMvcTest | 3 |
| DomainWorkflowIntegrationTest | 3 |
| UserControllerIntegrationTest | 9 |
| GraphQlIntegrationTest | 4 |
| JwtServiceTest | 3 |
| BookingServiceTest | 4 |
| PagingTest | 4 |
| StrongPasswordValidatorTest | 1 |

O Maven concluiu `verify` com `BUILD SUCCESS`, gerou o JAR executável e o relatório JaCoCo. Cobertura medida: 550 de 631 linhas (87,2%) e 154 de 265 ramificações (58,1%). Esses números não significam cobertura completa de todos os cenários.

Os testes de integração utilizaram H2 em memória, autenticação real e os filtros de segurança ativos. Não utilizaram o banco do projeto.

### Particularidade deste ambiente

O compilador padrão encontrou `AccessDeniedException` ao resolver caminhos de dependências JAR no ambiente automatizado Windows. Para executar a validação, foi usada uma cópia temporária com Eclipse Compiler 2.15.0 (adaptador Plexus), JDK 21.0.8 e source/target 21, sem a opção `release`. O código-fonte validado foi comparado byte a byte com o código entregue. Essa adaptação **não foi incorporada ao pom.xml entregue**, que mantém Java 21 e o compilador padrão. A execução padrão de `mvn verify` deve ser confirmada no terminal local ou no build Docker.

## SQL e containers

- As migrações V1, V2 e V3 passaram por 26 verificações em PostgreSQL compilado para WebAssembly (PGlite 0.5.10), incluindo preservação dos registros legados, unicidade, relacionamentos, restrições e consultas em lote.
- Esse teste não substitui a integração Flyway/JDBC com PostgreSQL 17 nativo.
- A sintaxe das composições de desenvolvimento e produção foi validada por `docker compose config`.
- O daemon Docker não estava disponível; o build das imagens e a execução conjunta dos containers permanecem pendentes de validação local.
- Não foram aplicadas migrações ao banco de trabalho nem realizadas exclusões de dados reais.

## Conferência local

### Comparação medida REST/GraphQL

Em 26/10/2026 às 22:00 (Brasília), uma instância temporária na porta 18080 executou a comparação com H2, um prestador e um serviço fictício. Foram três aquecimentos e 30 amostras alternadas por protocolo; o script confirmou a equivalência dos campos e registros.

| Protocolo | Média (ms) | Mediana (ms) | p95 (ms) | Corpo da resposta (bytes) |
|---|---:|---:|---:|---:|
| REST | 7,019 | 6,714 | 8,879 | 375 |
| GraphQL | 7,751 | 7,678 | 9,910 | 337 |

Nesse ensaio pequeno, REST apresentou menor latência média e GraphQL retornou um corpo menor. GraphQL enviou também um corpo de requisição de 268 bytes, enquanto o GET REST não enviou corpo. Os envelopes são diferentes: REST inclui paginação e GraphQL inclui `data`. A medição não inclui cabeçalhos nem throughput concorrente e não permite concluir qual alternativa será melhor em produção com PostgreSQL e maior carga. A API temporária foi encerrada após a comparação.

### Próximas verificações no ambiente real

1. Configure `.env` conforme o README e mantenha as credenciais do banco existente.
2. Execute `mvn verify` com Java 21.
3. Execute o Compose de desenvolvimento e confirme os healthchecks.
4. Rode a coleção Postman de continuação na ordem.
5. Execute a comparação REST/GraphQL e o roteiro EXPLAIN com dados representativos antes de concluir algo sobre desempenho em produção.
