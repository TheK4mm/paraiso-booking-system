-- =====================================================================
-- Hotel Paraíso V2.0 — Esquema base
-- Única fuente de verdad del esquema de dominio (gestionada por Flyway).
-- Todas las tablas incluyen columnas de auditoría uniformes; las tablas
-- con concurrencia de escritura incluyen columna version (lock optimista).
-- =====================================================================

-- ─── Secuencias para códigos de negocio (generación atómica) ─────────
CREATE SEQUENCE seq_codigo_reserva  START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_numero_factura  START WITH 1 INCREMENT BY 1;

-- ─── Catálogos ────────────────────────────────────────────────────────
CREATE TABLE tipos_habitacion (
    id                  BIGSERIAL       PRIMARY KEY,
    nombre              VARCHAR(80)     NOT NULL,
    descripcion         VARCHAR(500),
    capacidad_maxima    INTEGER         NOT NULL CHECK (capacidad_maxima > 0),
    precio_base_noche   NUMERIC(10,2)   NOT NULL CHECK (precio_base_noche > 0),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP,
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),
    CONSTRAINT uq_tipo_habitacion_nombre UNIQUE (nombre)
);

CREATE TABLE habitaciones (
    id                  BIGSERIAL       PRIMARY KEY,
    numero              VARCHAR(10)     NOT NULL,
    piso                INTEGER         NOT NULL CHECK (piso > 0),
    descripcion         VARCHAR(500),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'DISPONIBLE'
                        CHECK (estado IN ('DISPONIBLE','OCUPADA','MANTENIMIENTO','BLOQUEADA')),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    version             BIGINT          NOT NULL DEFAULT 0,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP,
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),
    tipo_habitacion_id  BIGINT          NOT NULL
                        REFERENCES tipos_habitacion(id) ON DELETE RESTRICT,
    CONSTRAINT uq_habitacion_numero UNIQUE (numero)
);

CREATE INDEX idx_habitacion_estado ON habitaciones(estado);
CREATE INDEX idx_habitacion_tipo   ON habitaciones(tipo_habitacion_id);

CREATE TABLE servicios (
    id                  BIGSERIAL       PRIMARY KEY,
    nombre              VARCHAR(100)    NOT NULL,
    descripcion         VARCHAR(500),
    precio              NUMERIC(10,2)   NOT NULL CHECK (precio >= 0),
    categoria           VARCHAR(30)     NOT NULL
                        CHECK (categoria IN ('ALIMENTACION','SPA_BIENESTAR','TRANSPORTE',
                                             'ENTRETENIMIENTO','LAVANDERIA','NEGOCIOS','OTROS')),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP,
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),
    CONSTRAINT uq_servicio_nombre UNIQUE (nombre)
);

-- ─── Personas ─────────────────────────────────────────────────────────
CREATE TABLE clientes (
    id                  BIGSERIAL       PRIMARY KEY,
    nombre              VARCHAR(100)    NOT NULL,
    apellido            VARCHAR(100)    NOT NULL,
    tipo_documento      VARCHAR(20)     NOT NULL,
    numero_documento    VARCHAR(30)     NOT NULL,
    email               VARCHAR(150)    NOT NULL,
    telefono            VARCHAR(20),
    direccion           VARCHAR(300),
    pais                VARCHAR(80),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP,
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),
    CONSTRAINT uq_cliente_documento UNIQUE (numero_documento),
    CONSTRAINT uq_cliente_email     UNIQUE (email)
);

CREATE TABLE empleados (
    id                  BIGSERIAL       PRIMARY KEY,
    nombre              VARCHAR(100)    NOT NULL,
    apellido            VARCHAR(100)    NOT NULL,
    numero_documento    VARCHAR(30)     NOT NULL,
    cargo               VARCHAR(80)     NOT NULL,
    email_corporativo   VARCHAR(150),
    telefono_extension  VARCHAR(10),
    fecha_contratacion  DATE            NOT NULL,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP,
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),
    CONSTRAINT uq_empleado_documento UNIQUE (numero_documento)
);

-- ─── Reservas ─────────────────────────────────────────────────────────
CREATE TABLE reservas (
    id                  BIGSERIAL       PRIMARY KEY,
    codigo_reserva      VARCHAR(20)     NOT NULL,
    fecha_entrada       DATE            NOT NULL,
    fecha_salida        DATE            NOT NULL,
    numero_huespedes    INTEGER         NOT NULL CHECK (numero_huespedes > 0),
    total_noches        INTEGER         NOT NULL CHECK (total_noches > 0),
    precio_total        NUMERIC(12,2)   NOT NULL CHECK (precio_total >= 0),
    observaciones       VARCHAR(500),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado IN ('PENDIENTE','CONFIRMADA','CHECKIN',
                                          'CHECKOUT','CANCELADA','NO_SHOW')),
    version             BIGINT          NOT NULL DEFAULT 0,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP,
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),
    cliente_id          BIGINT          NOT NULL
                        REFERENCES clientes(id) ON DELETE RESTRICT,
    empleado_id         BIGINT
                        REFERENCES empleados(id) ON DELETE SET NULL,
    CONSTRAINT uq_reserva_codigo UNIQUE (codigo_reserva),
    CONSTRAINT chk_reserva_fechas CHECK (fecha_salida > fecha_entrada)
);

CREATE INDEX idx_reserva_cliente  ON reservas(cliente_id);
CREATE INDEX idx_reserva_empleado ON reservas(empleado_id);
CREATE INDEX idx_reserva_estado   ON reservas(estado);
CREATE INDEX idx_reserva_entrada  ON reservas(fecha_entrada);
CREATE INDEX idx_reserva_salida   ON reservas(fecha_salida);
CREATE INDEX idx_reserva_creado   ON reservas(creado_en);

CREATE TABLE reserva_habitacion (
    reserva_id      BIGINT  NOT NULL REFERENCES reservas(id)      ON DELETE CASCADE,
    habitacion_id   BIGINT  NOT NULL REFERENCES habitaciones(id)  ON DELETE RESTRICT,
    PRIMARY KEY (reserva_id, habitacion_id)
);

CREATE INDEX idx_rh_habitacion ON reserva_habitacion(habitacion_id);

CREATE TABLE reserva_servicio (
    reserva_id  BIGINT  NOT NULL REFERENCES reservas(id)   ON DELETE CASCADE,
    servicio_id BIGINT  NOT NULL REFERENCES servicios(id)  ON DELETE RESTRICT,
    PRIMARY KEY (reserva_id, servicio_id)
);

CREATE INDEX idx_rs_servicio ON reserva_servicio(servicio_id);

-- ─── Facturación ──────────────────────────────────────────────────────
CREATE TABLE facturas (
    id                  BIGSERIAL       PRIMARY KEY,
    numero_factura      VARCHAR(30)     NOT NULL,
    subtotal            NUMERIC(12,2)   NOT NULL CHECK (subtotal >= 0),
    impuesto_porcentaje NUMERIC(5,2)    NOT NULL DEFAULT 19.00
                        CHECK (impuesto_porcentaje BETWEEN 0 AND 100),
    impuesto_valor      NUMERIC(12,2)   NOT NULL CHECK (impuesto_valor >= 0),
    descuento           NUMERIC(12,2)   NOT NULL DEFAULT 0.00 CHECK (descuento >= 0),
    total               NUMERIC(12,2)   NOT NULL CHECK (total >= 0),
    notas               VARCHAR(500),
    estado_factura      VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado_factura IN ('PENDIENTE','PAGADA_PARCIALMENTE','PAGADA','ANULADA')),
    fecha_emision       TIMESTAMP       NOT NULL DEFAULT NOW(),
    version             BIGINT          NOT NULL DEFAULT 0,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP,
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),
    reserva_id          BIGINT          NOT NULL
                        REFERENCES reservas(id) ON DELETE RESTRICT,
    CONSTRAINT uq_factura_numero  UNIQUE (numero_factura),
    CONSTRAINT uq_factura_reserva UNIQUE (reserva_id),
    CONSTRAINT chk_factura_descuento CHECK (descuento <= subtotal)
);

CREATE INDEX idx_factura_estado  ON facturas(estado_factura);
CREATE INDEX idx_factura_emision ON facturas(fecha_emision);

CREATE TABLE pagos (
    id                      BIGSERIAL       PRIMARY KEY,
    monto                   NUMERIC(12,2)   NOT NULL CHECK (monto > 0),
    metodo_pago             VARCHAR(20)     NOT NULL
                            CHECK (metodo_pago IN ('EFECTIVO','TARJETA_CREDITO','TARJETA_DEBITO',
                                                   'TRANSFERENCIA','PSE','NEQUI','DAVIPLATA')),
    referencia_transaccion  VARCHAR(100),
    estado_pago             VARCHAR(15)     NOT NULL DEFAULT 'PENDIENTE'
                            CHECK (estado_pago IN ('PENDIENTE','APROBADO','RECHAZADO',
                                                   'REEMBOLSADO','CANCELADO')),
    descripcion             VARCHAR(300),
    fecha_pago              TIMESTAMP       NOT NULL DEFAULT NOW(),
    version                 BIGINT          NOT NULL DEFAULT 0,
    creado_en               TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en          TIMESTAMP,
    creado_por              VARCHAR(100),
    actualizado_por         VARCHAR(100),
    reserva_id              BIGINT          NOT NULL REFERENCES reservas(id)  ON DELETE RESTRICT,
    factura_id              BIGINT          REFERENCES facturas(id) ON DELETE SET NULL
);

CREATE INDEX idx_pago_reserva ON pagos(reserva_id);
CREATE INDEX idx_pago_factura ON pagos(factura_id);
CREATE INDEX idx_pago_estado  ON pagos(estado_pago);
CREATE INDEX idx_pago_fecha   ON pagos(fecha_pago);
