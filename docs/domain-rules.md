# Serviços, contratações e avaliações

O código mantém `Controller → Service → Repository`. Os controllers definem o contrato HTTP;
as regras e autorizações também ficam nos services para serem reutilizadas no GraphQL.
Perfis públicos e avaliações não incluem e-mail, telefone, senha ou hash de senha.

## Catálogo e prestadores

- `GET /api/services`: catálogo público paginado; filtros `category` e `search` (título).
- `GET /api/services/{id}`: serviço ativo de um prestador ativo.
- `GET /api/providers` e `GET /api/providers/{id}`: somente `id`, `fullName`, `bio`, `avatarUrl`.
- `GET /api/providers/{id}/services`: ofertas públicas de um prestador, paginadas.
- `POST /api/services`: exige `PROVIDER`; o proprietário vem do JWT. Retorna `201`.
- `PUT /api/services/{id}`: substitui dados do serviço; somente proprietário ou `ADMIN`.
- `DELETE /api/services/{id}`: exclusão lógica; retorna `204`. Ordens ativas impedem a exclusão (`409`).

`ServiceCategory` e a restrição SQL aceitam o mesmo conjunto de categorias do schema de referência.
Preço positivo tem no máximo oito casas inteiras e duas decimais (`NUMERIC(10,2)`).
O texto é validado, sanitizado pelo componente comum e validado novamente nos limites relevantes.

## Contratação

`POST /api/requests` recebe somente:

```json
{
  "serviceId": 1,
  "scheduledAt": "2027-12-15T14:00:00",
  "notes": "Preferência pelo período da tarde"
}
```

Exige `CLIENT`. `scheduledAt` é opcional e, quando informado, deve estar no futuro.
O servidor obtém o cliente pelo JWT e copia `services.price` para `totalPrice`.
Não aceita preço, proprietário, cliente ou status arbitrários enviados pelo cliente.
Serviço ou prestador inativos não podem ser contratados, nem é permitido contratar o próprio serviço.

`GET /api/requests` lista somente ordens das quais o usuário é cliente ou prestador;
`ADMIN` pode ver todas. A mesma restrição se aplica à consulta por ID.

`PATCH /api/requests/{id}/status` recebe `{"status":"ACCEPTED"}` e aplica:

| Status atual | Próximo status permitido |
| --- | --- |
| `PENDING` | `ACCEPTED` ou `CANCELLED` |
| `ACCEPTED` | `IN_PROGRESS` ou `CANCELLED` |
| `IN_PROGRESS` | `COMPLETED` |
| `COMPLETED` / `CANCELLED` | Nenhum; estados terminais |

O prestador proprietário ou `ADMIN` avança a execução. O cliente proprietário pode cancelar
somente em `PENDING` ou `ACCEPTED`. Transição inválida retorna `409`; acesso de outra pessoa retorna `403`.

## Avaliações

- `POST /api/reviews` recebe `requestId`, `rating` (1 a 5) e `comment` opcional.
- Exige cliente proprietário de ordem `COMPLETED`. Uma avaliação por ordem (`UNIQUE(request_id)`).
- Avaliar ordem não concluída ou repetir avaliação retorna `409`; avaliar ordem alheia retorna `403`.
- `GET /api/services/{serviceId}/reviews` retorna avaliações públicas paginadas.

## Concorrência e histórico

Contratação trava cliente e prestador por ID em ordem crescente, e depois trava o serviço.
Edição/exclusão do serviço e exclusão lógica do usuário usam os mesmos locks dos registros
envolvidos. A checagem de ordens ativas ocorre dentro da mesma transação que a exclusão.
Assim, uma contratação concorrente não pode entrar entre a verificação e a desativação.
Alterações de status e avaliações usam lock da ordem; a restrição `UNIQUE` também impede
avaliações duplicadas no banco.

Serviços e usuários são desativados, preservando referências, preço contratado e histórico.
Mudar o preço do catálogo não altera ordens antigas. Não há exclusão física em cascata.

## Evolução do banco e índices

`V1` permanece intacta. `V2` detecta previamente duplicatas em `LOWER(TRIM(email))` e interrompe
a transação com mensagem clara, sem remover usuários. Resolva os registros em conflito de forma
controlada antes de tentar novamente. A migração normaliza os e-mails, renomeia `user_type` para
`role`, adiciona `bio`/`avatar_url` e cria as novas tabelas e restrições.

`V3` adiciona índices parciais e compostos para catálogo, histórico e busca de ordens ativas;
remove o índice comum de e-mail redundante com o `UNIQUE`. O script
`docs/database/EXPLAIN_queries.sql` executa consultas somente leitura com `EXPLAIN (ANALYZE, BUFFERS)`.
Tempos e planos dependem do volume e das estatísticas do ambiente; não há resultados fabricados.

O batch do GraphQL consulta no máximo 100 prestadores por vez e até 20 ofertas por prestador,
com `ROW_NUMBER()` no SQL. Os dados públicos são projetados em uma única consulta, sem N+1.
