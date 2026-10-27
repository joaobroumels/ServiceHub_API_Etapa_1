-- O índice UNIQUE de email já atende igualdade e autenticação; evita manter uma cópia.
DROP INDEX idx_users_email;

-- Catálogo de prestadores e ofertas: predicados parciais excluem registros inativos.
CREATE INDEX idx_users_active_providers ON users(id) WHERE is_active AND role = 'PROVIDER';
CREATE INDEX idx_services_active_provider_id ON services(provider_id, id) WHERE is_active;
DROP INDEX idx_services_category;
CREATE INDEX idx_services_active_category_id ON services(category, id) WHERE is_active;

-- Ordens ativas bloqueiam exclusões. Índices parciais evitam varrer todo o histórico.
CREATE INDEX idx_requests_active_service ON service_requests(service_id)
    WHERE status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS');
CREATE INDEX idx_requests_active_client ON service_requests(client_id)
    WHERE status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS');

-- Histórico paginado por cliente; o índice de service_id também atende o JOIN do prestador.
CREATE INDEX idx_requests_client_id ON service_requests(client_id, id);

-- UNIQUE(request_id) já é o índice para verificar avaliações duplicadas e juntar com ordens.
-- Consulte docs/database/EXPLAIN_queries.sql para observar os planos no seu conjunto de dados.
