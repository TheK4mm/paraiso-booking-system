-- =====================================================================
-- Hotel Paraíso V2.1 — Portal público: rol CLIENTE, verificación de
-- email y presentación pública de tipos de habitación
-- =====================================================================

-- Rol CLIENTE (el CHECK de V2 era inline: PG lo nombró usuarios_rol_check)
ALTER TABLE usuarios DROP CONSTRAINT usuarios_rol_check;
ALTER TABLE usuarios ADD CONSTRAINT chk_usuarios_rol
    CHECK (rol IN ('ADMIN','RECEPCIONISTA','CLIENTE'));

-- Los clientes inician sesión con su email (username = email)
ALTER TABLE usuarios ALTER COLUMN username TYPE VARCHAR(150);

-- Verificación de email: las cuentas existentes quedan verificadas
ALTER TABLE usuarios ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT TRUE;

-- Vínculo cuenta ↔ ficha de cliente (1:1 opcional, solo cuentas CLIENTE)
ALTER TABLE usuarios ADD COLUMN cliente_id BIGINT REFERENCES clientes(id);
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_cliente UNIQUE (cliente_id);

-- Tokens de verificación de email. Llevan el payload del cliente
-- pendiente: la ficha de Cliente se crea solo tras verificar, así que
-- sus datos deben sobrevivir entre el registro y la verificación.
CREATE TABLE tokens_verificacion_email (
    id               BIGSERIAL       PRIMARY KEY,
    token_hash       VARCHAR(64)     NOT NULL,
    expira_en        TIMESTAMP       NOT NULL,
    usado_en         TIMESTAMP,
    creado_en        TIMESTAMP       NOT NULL DEFAULT NOW(),
    usuario_id       BIGINT          NOT NULL
                     REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre           VARCHAR(100)    NOT NULL,
    apellido         VARCHAR(100)    NOT NULL,
    tipo_documento   VARCHAR(20)     NOT NULL,
    numero_documento VARCHAR(30)     NOT NULL,
    telefono         VARCHAR(20),
    CONSTRAINT uq_tve_token UNIQUE (token_hash)
);

CREATE INDEX idx_tve_usuario ON tokens_verificacion_email(usuario_id);

-- Presentación pública de los tipos de habitación (NULL-tolerante:
-- prod puede operar sin fotos; la vista usa un placeholder)
ALTER TABLE tipos_habitacion ADD COLUMN imagen VARCHAR(255);
ALTER TABLE tipos_habitacion ADD COLUMN comodidades VARCHAR(500);
