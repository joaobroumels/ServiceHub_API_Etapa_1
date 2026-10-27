-- Evolui V1 sem reescrever seu histórico e sem descartar registros existentes.
-- Se houver duplicatas após normalização, a transação inteira falha antes do UPDATE.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM users GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Migração interrompida: há e-mails duplicados após LOWER(TRIM(email)). Resolva as duplicatas preservando os registros e execute novamente.';
    END IF;
END $$;

UPDATE users SET email = LOWER(TRIM(email));
ALTER TABLE users DROP CONSTRAINT chk_users_user_type;
DROP INDEX idx_users_user_type;
ALTER TABLE users RENAME COLUMN user_type TO role;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'CLIENT';
ALTER TABLE users ADD COLUMN bio TEXT;
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);
ALTER TABLE users RENAME CONSTRAINT users_email_key TO uk_users_email;
ALTER TABLE users ADD CONSTRAINT chk_users_email_low CHECK (email = LOWER(TRIM(email)));
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'PROVIDER', 'CLIENT'));

COMMENT ON TABLE users IS 'Usuários da plataforma. Papel único por usuário.';
COMMENT ON COLUMN users.password_hash IS 'Hash BCrypt; nunca é exposto em respostas da API.';
COMMENT ON COLUMN users.is_active IS 'Exclusão lógica: usuário inativo não autentica e seu JWT é rejeitado.';

CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    provider_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_services_provider FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_services_price CHECK (price > 0),
    CONSTRAINT chk_services_category CHECK (category IN (
        'LIMPEZA', 'DIARISTA', 'ENCANAMENTO', 'ELETRICA', 'JARDINAGEM', 'PINTURA',
        'MARCENARIA', 'PEDREIRO', 'DEDETIZACAO', 'AR_CONDICIONADO', 'CHAVEIRO',
        'MONTAGEM_MOVEIS', 'MUDANCA', 'SUPORTE_TECNICO', 'MANUTENCAO_COMPUTADORES',
        'DESENVOLVIMENTO_SOFTWARE', 'REDES_E_INFRAESTRUTURA', 'SEGURANCA_DIGITAL',
        'RECUPERACAO_DE_DADOS', 'CONSULTORIA_TI', 'OUTROS'))
);
CREATE INDEX idx_services_provider ON services(provider_id);
CREATE INDEX idx_services_category ON services(category) WHERE is_active;
COMMENT ON COLUMN services.category IS 'Valores sincronizados com o enum ServiceCategory; adicionar valores exige nova migração.';

CREATE TABLE service_requests (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMP,
    notes TEXT,
    total_price NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_requests_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT,
    CONSTRAINT fk_requests_client FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_requests_price CHECK (total_price > 0)
);
CREATE INDEX idx_requests_service ON service_requests(service_id);
CREATE INDEX idx_requests_client ON service_requests(client_id);
CREATE INDEX idx_requests_status ON service_requests(status);
COMMENT ON COLUMN service_requests.total_price IS 'Snapshot do preço no momento da contratação; não é atualizado quando o serviço muda.';
COMMENT ON COLUMN service_requests.status IS 'Transições e permissões são verificadas pela camada de serviço.';

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_reviews_request UNIQUE(request_id),
    CONSTRAINT fk_reviews_request FOREIGN KEY (request_id) REFERENCES service_requests(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
CREATE INDEX idx_reviews_reviewer ON reviews(reviewer_id);
