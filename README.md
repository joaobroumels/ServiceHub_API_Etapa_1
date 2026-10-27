# ServiceHub

API acadêmica para clientes encontrarem prestadores, consultarem serviços e acompanharem solicitações. Evolução do checkpoint inicial com autenticação JWT, autorização por papel e proprietário, testes, Docker e uma interface GraphQL complementar ao REST.

Repositório: [joaobroumels/ServiceHub_API_Etapa_1](https://github.com/joaobroumels/ServiceHub_API_Etapa_1).

## Funcionalidades

- Cadastro e login de clientes e prestadores com JWT, senhas BCrypt e administração opcional por bootstrap.
- Autorização por papel e proprietário, perfis de usuário e desativação de contas.
- Catálogo paginado de prestadores e serviços, contratação, acompanhamento de solicitações e avaliações.
- API REST documentada com OpenAPI/Swagger e consultas/mutation GraphQL.
- Migrações Flyway, índices PostgreSQL, testes automatizados e relatório JaCoCo.
- Containers para API e PostgreSQL com volume persistente e healthchecks.

Para obter o código:

```bash
git clone https://github.com/joaobroumels/ServiceHub_API_Etapa_1.git
cd ServiceHub_API_Etapa_1
```

## Requisitos

- Java 21 e Maven 3.9+ para desenvolver ou executar testes.
- Docker com Docker Compose para a execução completa em containers.
- PostgreSQL 17 para execução local. Os testes automatizados utilizam H2 isolado.

## Configurar o ambiente

Se `.env` ainda não existir, copie `.env.example` para `.env` e substitua os placeholders. Preserve um `.env` existente e as credenciais do banco já inicializado. O arquivo `.env` está no `.gitignore` e não deve ser publicado. `DB_PASSWORD` e `JWT_SECRET` são obrigatórios na execução; não há segredo JWT de produção embutido no código.

Gere um segredo JWT com **32 bytes aleatórios em Base64**. No PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Ou utilize `openssl rand -base64 32`. Cole o resultado em `JWT_SECRET` no seu `.env`. Não reutilize o segredo fixo dos testes. O token expira em 24 horas por padrão (`JWT_EXPIRATION_SECONDS=86400`).

Para criar o primeiro administrador, preencha `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` em conjunto antes de iniciar. Esse bootstrap não promove uma conta comum existente. Após a criação, deixe as duas variáveis vazias e reinicie a API. Cadastro público aceita apenas `CLIENT` e `PROVIDER`.

## Executar tudo com Docker

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail 100 api postgres
```

Confirme que os dois serviços estão `healthy` e que `http://localhost:8080/actuator/health` retorna HTTP 200 com `{"status":"UP"}`. Use `config --quiet` para validar sem imprimir as variáveis resolvidas, que incluem segredos.

A API atende em `http://localhost:8080`. O PostgreSQL é acessível somente pela rede interna `servicehub`, pelo host `postgres:5432`. O Compose espera o banco ficar saudável antes de iniciar a API; os dois serviços possuem healthcheck e `restart: unless-stopped`. O Dockerfile utiliza build Maven multi-stage, Java 21 e usuário sem privilégios de root no runtime.

Para o ambiente de desenvolvimento, com o banco também acessível pelo computador em `127.0.0.1:5433`:

```bash
docker compose -f compose.yaml -f compose.dev.yaml up -d --build
```

Para uma configuração de produção, com Swagger desabilitado e banco sem porta publicada:

```bash
docker compose -f compose.yaml -f compose.prod.yaml up -d --build
```

Configure HTTPS no proxy de entrada e use valores próprios no ambiente. Esse arquivo é uma configuração de container, não um provisionamento de infraestrutura pública.

### Preservar o banco do checkpoint

O serviço continua chamado `postgres`, o container continua `servicehub-postgres` e o volume lógico continua `servicehub_postgres_data`. O nome físico do volume depende do nome de projeto do Compose. Se você já usava Docker, confira `docker volume ls` e mantenha o mesmo diretório/nome de projeto, ou use `docker compose -p NOME_ORIGINAL ...`. Não execute `down -v`, pois isso remove os dados. Não altere a versão principal do PostgreSQL de um volume existente sem uma migração apropriada.

Renomear o repositório ou a pasta local não renomeia o volume. Para manter o projeto antigo após mudar de pasta, defina `COMPOSE_PROJECT_NAME=NOME_ORIGINAL` somente no `.env` local. Faça backup antes das migrações e mantenha dumps e credenciais fora do Git.

As variáveis `POSTGRES_*` criam credenciais apenas ao inicializar um volume vazio. Em um banco já existente, `DB_PASSWORD` precisa corresponder à senha desse banco; mudar apenas o `.env` não redefine a senha do PostgreSQL. O Flyway aplica as novas migrações preservando os registros do primeiro checkpoint. Não execute `servicehub_schema.sql` manualmente sobre o banco migrado.

## Desenvolver no VS Code / executar pelo Maven

Inicie apenas o banco, preservando a porta local do checkpoint:

```bash
docker compose -f compose.yaml -f compose.dev.yaml up -d postgres
```

O Spring Boot não carrega `.env` automaticamente fora do Compose. Configure `DB_PASSWORD` e `JWT_SECRET` nas variáveis de ambiente do terminal/IDE e, se necessário, `DB_USERNAME` e `DB_URL`:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5433/servicehub"
$env:DB_USERNAME = "servicehub"
$env:DB_PASSWORD = "SUA_SENHA_DO_BANCO"
$env:JWT_SECRET = "SEU_SEGREDO_BASE64_DE_32_BYTES"
mvn spring-boot:run
```

Não execute simultaneamente a API local e a API do Compose na mesma porta 8080. Para voltar ao checkpoint, faça backup do banco antes; as novas migrações não são revertidas automaticamente.

## Documentação e saúde

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Saúde: <http://localhost:8080/actuator/health>
- Endpoint GraphQL: `POST http://localhost:8080/graphql`

No Swagger, execute o login, copie `accessToken` e use **Authorize** para informar o token Bearer. A resposta do usuário não expõe senha nem hash. O healthcheck é público e apresenta apenas o estado do serviço.

## Autenticação e autorização

Cadastro público (`POST /api/auth/register`):

```json
{
  "fullName": "Maria Silva",
  "email": "maria@servicehub.com",
  "password": "Senha123",
  "phone": "62999999999",
  "role": "CLIENT",
  "bio": null,
  "avatarUrl": null
}
```

A senha exige no mínimo 8 caracteres, ao menos uma letra maiúscula e um número, com limite de 72 bytes para BCrypt. `userType` continua aceito como alias legado de `role`. Clientes e prestadores fazem login com `POST /api/auth/login`:

```json
{"email": "maria@servicehub.com", "password": "Senha123"}
```

O login devolve `accessToken`, `tokenType: "Bearer"`, `expiresIn` e o usuário público. Envie `Authorization: Bearer SEU_TOKEN` nos endpoints protegidos. Tokens adulterados, expirados ou de usuários desativados são recusados. `401` significa ausência/falha de autenticação e `403` significa falta de permissão para aquela operação.

| Recurso | Acesso |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Público; cadastro nunca cria ADMIN |
| `GET /api/users/me` | Usuário autenticado |
| `POST /api/users`, `GET /api/users` | ADMIN; listagem paginada |
| `GET/PUT/DELETE /api/users/{id}` | Próprio usuário ou ADMIN |
| `GET /api/services`, `GET /api/services/{id}` | Público |
| `POST /api/services` | PROVIDER; prestador é obtido do token |
| `PUT/DELETE /api/services/{id}` | Prestador proprietário ou ADMIN |
| `POST /api/requests` | CLIENT; cliente é obtido do token |
| `GET /api/requests`, `GET /api/requests/{id}` | Partes envolvidas ou ADMIN |
| `PATCH /api/requests/{id}/status` | Partes autorizadas conforme transição |
| `POST /api/reviews` | CLIENT dono da solicitação concluída |
| `GET /api/services/{id}/reviews` | Público |

Um usuário comum não pode promover a própria conta. A exclusão de usuário é lógica (desativação), invalida o acesso e é bloqueada quando há solicitações ativas. As regras de propriedade são verificadas no servidor, mesmo se o cliente alterar IDs no JSON ou na URL.

## Serviços, solicitações e avaliações

Um prestador cadastra um serviço com `title`, `description`, `price` e `category`. Um cliente solicita esse serviço:

```json
{
  "serviceId": 1,
  "scheduledAt": "2027-01-15T14:00:00",
  "notes": "Atendimento no período da tarde"
}
```

O servidor preenche cliente, prestador, valor e status. A solicitação inicia em `PENDING`. O prestador responsável avança `PENDING → ACCEPTED → IN_PROGRESS → COMPLETED`. O cliente pode cancelar em `PENDING` ou `ACCEPTED`; transições incompatíveis são recusadas. A avaliação (`requestId`, `rating` de 1 a 5 e `comment`) só é permitida após `COMPLETED`, pelo cliente da solicitação, e uma única vez.

As listagens REST oferecem paginação (`page` e `size`) para evitar respostas sem limite. Exemplo: `GET /api/services?page=0&size=10`.

## GraphQL

A interface GraphQL complementa o REST, reutilizando as mesmas regras de negócio. Consulta pública para escolher campos e navegar entre prestadores e serviços:

```graphql
query {
  providers(first: 10) {
    id
    fullName
    bio
    services { id title price category }
  }
}
```

Outra consulta é `servicesByProvider(providerId: "1", first: 10)`. Os limites evitam consultas sem paginação. Para criar uma solicitação, faça login como CLIENT e envie o token no cabeçalho:

```graphql
mutation {
  bookService(input: {
    serviceId: "1"
    scheduledAt: "2027-01-15T14:00:00"
    notes: "Solicitação pelo GraphQL"
  }) {
    id
    status
    totalPrice
  }
}
```

Envie `{ "query": "..." }` como JSON para `/graphql`. No GraphQL, erros de negócio aparecem em `errors[].extensions.code`; portanto, um HTTP 200 por si só não comprova sucesso da operação. As relações de prestador/serviços usam carregamento em lote para evitar N+1.

## Testes

```bash
mvn test
mvn verify
mvn clean verify
```

Confira os resultados em `target/surefire-reports/` e a cobertura em `target/site/jacoco/index.html`. O Dockerfile executa `package -DskipTests`; o build Docker não substitui `mvn verify`.

Os testes utilizam um banco H2 em memória, segredo JWT exclusivo de teste e transações revertidas entre cenários. Não conectam ao banco do usuário nem exigem containers. A suíte cobre autenticação real, HTTP, autorização, validação, exclusão lógica, fluxo de solicitações/avaliações e GraphQL. A validação de migrações e compatibilidade PostgreSQL deve ser realizada em um banco descartável separado antes de promover uma versão.

### Comparar REST e GraphQL

Com a API em execução e ao menos um prestador com serviços cadastrados, substitua `ID_DO_PRESTADOR` pelo ID real:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Compare-RestGraphql.ps1 -ProviderId ID_DO_PRESTADOR -Iterations 30
```

O script confere a equivalência dos dados, aquece as consultas e alterna amostras REST/GraphQL. Grava medições reais em CSV/JSON na pasta local `performance-results/`; não inclui resultados pré-fabricados. O parâmetro `ExecutionPolicy` vale somente para esse processo, sem alterar permanentemente a política do Windows. Compare latência e tamanho da resposta no mesmo ambiente; um transporte não é necessariamente mais rápido em todos os cenários.

### Postman

Importe `postman/ServiceHub_API_Continuacao.postman_collection.json` e `postman/ServiceHub_Local_Continuacao.postman_environment.json`, selecione o ambiente e execute na ordem da coleção. Ela cobre autenticação, papéis, serviços, solicitações, avaliações e GraphQL. Se um fluxo exigir ADMIN, use as credenciais criadas pelo bootstrap no seu ambiente local. Não publique tokens nem senhas reais ao exportar variáveis.

## Organização

```text
src/main/java/com/servicehub/
  config/        Configurações de OpenAPI e aplicação
  controller/    Endpoints REST: recebem entrada e delegam
  dto/           Contratos de entrada/saída e validações
  exception/     Respostas padronizadas de erros
  graphql/       Resolvers e integração GraphQL
  model/         Entidades JPA e enumerações
  repository/    Consultas e persistência
  security/      JWT, filtro, autorização e usuário autenticado
  service/       Regras de negócio, transações e autenticação
src/main/resources/
  db/migration/  Migrações incrementais Flyway
  graphql/       Schema GraphQL
src/test/        Testes automatizados e configuração H2
postman/         Coleções e ambiente para testes manuais
```

O fluxo principal é `Controller → Service → Repository → PostgreSQL`. DTOs separam o contrato público das entidades. Campos textuais são validados/sanitizados e as senhas são armazenadas com BCrypt; autenticação não equivale a criptografar o transporte, por isso a publicação deve utilizar HTTPS.
