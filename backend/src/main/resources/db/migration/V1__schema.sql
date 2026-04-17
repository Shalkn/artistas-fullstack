CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

CREATE TABLE artist (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_artist_name ON artist (name);

CREATE TABLE album (
    id BIGSERIAL PRIMARY KEY,
    artist_id BIGINT NOT NULL REFERENCES artist (id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_album_artist ON album (artist_id);
CREATE INDEX idx_album_title ON album (title);

CREATE TABLE album_cover (
    id BIGSERIAL PRIMARY KEY,
    album_id BIGINT NOT NULL REFERENCES album (id) ON DELETE CASCADE,
    object_key VARCHAR(1024) NOT NULL,
    content_type VARCHAR(120),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_album_cover_album ON album_cover (album_id);

-- Histórico de denominações por código externo da API Argus
CREATE TABLE regional (
    id BIGSERIAL PRIMARY KEY,
    codigo_externo INTEGER NOT NULL,
    nome VARCHAR(200) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_regional_codigo_ativo ON regional (codigo_externo, ativo);
