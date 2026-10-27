# Continuação do ServiceHub

Esta etapa parte do CRUD de usuários do checkpoint. As aulas são a referência funcional; exemplos didáticos de outros recursos foram adaptados ao domínio ServiceHub.

| Material | Implementação nesta etapa |
|---|---|
| Aula 08 | Spring Security, cadeia stateless, BCrypt, Bean Validation, senha forte, erros JSON 400/401/403/404/409, sanitização OWASP, CORS explícito e autorização por papel/proprietário |
| Aula 09 | Login JWT HS256 com expiração e roles, validação a cada requisição, GraphQL como opção A, queries de prestadores/serviços e mutation de contratação |
| Aula 10 | JUnit 5, Mockito, teste unitário de regras, `@WebMvcTest`, MockMvc com segurança ativa, integração Spring/H2 e relatório JaCoCo |
| Aula 11 | Índices parciais e compostos em V3, consultas parametrizadas, paginação limitada, busca em lote e roteiro `EXPLAIN (ANALYZE, BUFFERS)` |
| Aula 12 | Dockerfile Java 21 multi-stage, Compose API + PostgreSQL, volume persistente, rede interna, healthchecks, dependência saudável, variáveis de ambiente e variantes dev/prod |
| servicehub_schema.sql | Migrações incrementais: `users.role`, biografia/avatar, serviços, solicitações e avaliações com restrições e relacionamentos |

A aula 09 oferece GraphQL **ou** gRPC. Esta implementação utiliza GraphQL. Tópicos apresentados como próximos passos, como refresh tokens, microsserviços, Prometheus/Grafana, Swarm e Kubernetes, não integram a entrega desta etapa.

## O que mudou em relação ao primeiro checkpoint

- Cadastro público: `POST /api/auth/register`. O `POST /api/users` agora exige ADMIN.
- Login: `POST /api/auth/login`; usar `accessToken` no cabeçalho `Authorization: Bearer ...`.
- `role` substitui `userType` no banco e nos contratos novos. O JSON antigo `userType` ainda é aceito como alias de entrada e devolvido como alias de resposta.
- Senha nova: pelo menos oito caracteres, uma maiúscula e um número; máximo 72 bytes UTF-8 do BCrypt. Senhas antigas continuam válidas para login se corresponderem ao hash existente.
- Usuários são desativados, preservando histórico; contas inativas não fazem login nem reutilizam JWT.
- O perfil pertence ao titular ou ao ADMIN. Papéis não mudam pela edição de perfil.
- Listagens retornam `content`, `page`, `size`, `totalElements`, `totalPages` e `last`. O limite é 100 itens e a ordenação possui campos permitidos.
- O identificador de cliente/prestador vem do token, não de um ID fornecido livremente no corpo.

## Executar e demonstrar

Siga a configuração de `.env.example` no README. Não utilize o segredo fixo dos testes para iniciar a API normal. O primeiro administrador é opcional e é criado somente pelas variáveis de bootstrap documentadas.

```sh
docker compose -f compose.yaml -f compose.dev.yaml up -d --build
```

Use o mesmo nome de projeto Compose do banco antigo para reutilizar o volume. Antes de aplicar novas migrações em um banco importante, mantenha um backup. Não execute o schema de referência sobre o banco existente nem use `down -v` para atualizar a aplicação.

Importe os arquivos `ServiceHub_API_Continuacao.postman_collection.json` e `ServiceHub_Local_Continuacao.postman_environment.json` da pasta `postman`. Selecione o ambiente e rode a coleção nova na ordem. Ela cria contas fictícias com e-mails únicos, captura tokens/IDs e percorre contratação, conclusão e avaliação. A coleção antiga permanece como registro do checkpoint anterior.

Para a comparação exigida pela aula 09, crie um prestador com serviços e execute `scripts/Compare-RestGraphql.ps1` com os parâmetros descritos no README. O script compara campos equivalentes e gera resultados medidos; os tempos dependem do ambiente. Não interprete números de H2 ou de desenvolvimento como desempenho de produção.

Para a aula 11, consulte `docs/database/EXPLAIN_queries.sql`. Avalie o plano com dados representativos; uma tabela pequena pode legitimamente usar varredura sequencial mesmo com índices.

```sh
mvn verify
```

O relatório de cobertura fica em `target/site/jacoco/index.html`. A suíte H2 valida o comportamento da aplicação; a validação de migrações PostgreSQL e o build Docker devem usar instâncias descartáveis e nunca o banco de trabalho como fixture.
