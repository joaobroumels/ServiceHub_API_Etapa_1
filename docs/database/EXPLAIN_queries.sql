-- Diagnóstico somente leitura, destinado ao psql em um banco de desenvolvimento.
-- Exemplo: psql "$DATABASE_URL" -f docs/database/EXPLAIN_queries.sql
-- Troque os IDs por registros do seu ambiente. Não contém resultados ou tempos inventados.
-- Compare planos com dados representativos e estatísticas atualizadas; tabelas pequenas
-- podem escolher Seq Scan legitimamente. Não force enable_seqscan=off para provar ganho.
\set provider_id 1
\set client_id 2
\set service_id 1

BEGIN READ ONLY;

-- Catálogo por categoria + paginação: idx_services_active_category_id.
EXPLAIN (ANALYZE, BUFFERS)
SELECT s.id, s.title, s.price, s.category
  FROM services s JOIN users p ON p.id = s.provider_id
 WHERE s.is_active AND p.is_active AND p.role = 'PROVIDER'
   AND s.category = 'MANUTENCAO_COMPUTADORES'
 ORDER BY s.id LIMIT 20;

-- Catálogo de um prestador: idx_services_active_provider_id.
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, price FROM services
 WHERE provider_id = :provider_id AND is_active ORDER BY id LIMIT 20;

-- Busca de ordens que bloqueiam exclusão: índice parcial + JOIN pelo prestador.
EXPLAIN (ANALYZE, BUFFERS)
SELECT EXISTS (
    SELECT 1 FROM service_requests r JOIN services s ON s.id = r.service_id
     WHERE s.provider_id = :provider_id AND r.status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS')
);

EXPLAIN (ANALYZE, BUFFERS)
SELECT EXISTS (
    SELECT 1 FROM service_requests
     WHERE client_id = :client_id AND status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS')
);

-- Histórico paginado: idx_requests_client_id.
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, service_id, status, total_price FROM service_requests
 WHERE client_id = :client_id ORDER BY id LIMIT 20;

-- Avaliações por serviço: idx_requests_service + UNIQUE reviews(request_id).
EXPLAIN (ANALYZE, BUFFERS)
SELECT r.id, r.rating, r.comment FROM reviews r
 JOIN service_requests sr ON sr.id = r.request_id
 WHERE sr.service_id = :service_id ORDER BY r.id LIMIT 20;

COMMIT;
