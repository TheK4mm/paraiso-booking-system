-- =====================================================================
-- Hotel Paraíso V2.0 — Seguridad y registro de actividades
-- =====================================================================

CREATE TABLE usuarios (
    id                  BIGSERIAL       PRIMARY KEY,
    username            VARCHAR(50)     NOT NULL,
    email               VARCHAR(150)    NOT NULL,
    password_hash       VARCHAR(100)    NOT NULL,
    nombre_completo     VARCHAR(150)    NOT NULL,
    rol                 VARCHAR(20)     NOT NULL
                        CHECK (rol IN ('ADMIN','RECEPCIONISTA')),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP,
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT uq_usuario_email    UNIQUE (email)
);

CREATE TABLE password_reset_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    token_hash  VARCHAR(64)     NOT NULL,
    expira_en   TIMESTAMP       NOT NULL,
    usado_en    TIMESTAMP,
    creado_en   TIMESTAMP       NOT NULL DEFAULT NOW(),
    usuario_id  BIGINT          NOT NULL
                REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_prt_token UNIQUE (token_hash)
);

CREATE INDEX idx_prt_usuario ON password_reset_tokens(usuario_id);

CREATE TABLE activity_log (
    id           BIGSERIAL      PRIMARY KEY,
    username     VARCHAR(100)   NOT NULL,
    accion       VARCHAR(60)    NOT NULL,
    tipo_entidad VARCHAR(40),
    entidad_id   BIGINT,
    detalle      VARCHAR(500),
    creado_en    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_username ON activity_log(username);
CREATE INDEX idx_activity_creado   ON activity_log(creado_en);
CREATE INDEX idx_activity_entidad  ON activity_log(tipo_entidad, entidad_id);
