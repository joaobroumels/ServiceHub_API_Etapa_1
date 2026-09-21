# ServiceHub API

Projeto acadêmico desenvolvido com Java 21 e Spring Boot 3 para gerenciar clientes, prestadores e serviços.

## Requisitos

- Java 21
- Maven 3.9+
- Docker com Docker Compose, ou PostgreSQL instalado localmente

## Executar o projeto

1. Inicie o PostgreSQL:

   ```bash
   docker compose up -d
   ```

2. Inicie a API:

   ```bash
   mvn spring-boot:run
   ```

3. Acesse:

   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Teste da API: http://localhost:8080/api/hello

## Primeiro CRUD

| Método | Endpoint | Resultado esperado |
|---|---|---|
| POST | `/api/users` | Cadastra e retorna `201 Created` |
| GET | `/api/users` | Lista e retorna `200 OK` |
| GET | `/api/users/{id}` | Busca ou retorna `404 Not Found` |
| PUT | `/api/users/{id}` | Atualiza ou retorna `404 Not Found` |
| DELETE | `/api/users/{id}` | Exclui e retorna `204 No Content` |

Exemplo de cadastro:

```json
{
  "fullName": "Maria Silva",
  "email": "maria@servicehub.com",
  "password": "senha123",
  "phone": "62999999999",
  "userType": "CLIENT"
}
```

## Testes

```bash
mvn test
```
