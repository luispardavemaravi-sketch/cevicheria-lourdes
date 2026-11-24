-- Esquema inicial del Sistema de Pedidos y Caja - Cevicheria Lourdes
-- Montos en NUMERIC (nunca float). Fechas en TIMESTAMPTZ. Reglas de negocio reforzadas con CHECK e indices unicos parciales.

CREATE TABLE usuario (
  id                BIGSERIAL PRIMARY KEY,
  nombre            VARCHAR(60)  NOT NULL,
  username          VARCHAR(30)  NOT NULL UNIQUE,
  pin_hash          VARCHAR(72)  NOT NULL,
  rol               VARCHAR(10)  NOT NULL CHECK (rol IN ('ADMIN', 'MOZO', 'COCINA')),
  activo            BOOLEAN      NOT NULL DEFAULT TRUE,
  intentos_fallidos SMALLINT     NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
  bloqueado_hasta   TIMESTAMPTZ
);

CREATE TABLE mesa (
  numero SMALLINT PRIMARY KEY CHECK (numero BETWEEN 1 AND 14)
);

CREATE TABLE producto (
  id              BIGSERIAL PRIMARY KEY,
  nombre          VARCHAR(60)  NOT NULL UNIQUE,
  categoria       VARCHAR(20)  NOT NULL CHECK (categoria IN ('PLATO', 'BEBIDA', 'TAPER')),
  precio          NUMERIC(8,2) CHECK (precio > 0),                -- NULL cuando el precio va en presentacion
  requiere_cocina BOOLEAN      NOT NULL DEFAULT TRUE,
  disponible      BOOLEAN      NOT NULL DEFAULT TRUE,
  activo          BOOLEAN      NOT NULL DEFAULT TRUE,
  CHECK (categoria = 'BEBIDA' OR precio IS NOT NULL)
);

CREATE TABLE presentacion (
  id          BIGSERIAL PRIMARY KEY,
  producto_id BIGINT       NOT NULL REFERENCES producto (id),
  nombre      VARCHAR(30)  NOT NULL,
  precio      NUMERIC(8,2) NOT NULL CHECK (precio > 0),
  UNIQUE (producto_id, nombre)
);

CREATE TABLE acompanamiento (
  id     BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE producto_acompanamiento (
  producto_id       BIGINT NOT NULL REFERENCES producto (id),
  acompanamiento_id BIGINT NOT NULL REFERENCES acompanamiento (id),
  PRIMARY KEY (producto_id, acompanamiento_id)
);
CREATE INDEX ix_prod_acomp_acomp ON producto_acompanamiento (acompanamiento_id);

CREATE TABLE caja (
  id               BIGSERIAL PRIMARY KEY,
  fecha            DATE          NOT NULL UNIQUE,                 -- RN-11: una caja por dia
  monto_inicial    NUMERIC(10,2) NOT NULL CHECK (monto_inicial >= 0),
  estado           VARCHAR(10)   NOT NULL CHECK (estado IN ('ABIERTA', 'CERRADA')),
  efectivo_contado NUMERIC(10,2) CHECK (efectivo_contado >= 0),
  diferencia       NUMERIC(10,2),
  observacion      VARCHAR(200),
  abierta_en       TIMESTAMPTZ   NOT NULL,
  cerrada_en       TIMESTAMPTZ,
  CHECK ((estado = 'ABIERTA' AND cerrada_en IS NULL) OR (estado = 'CERRADA' AND cerrada_en IS NOT NULL))
);

CREATE TABLE cuenta (
  id             BIGSERIAL PRIMARY KEY,
  tipo           VARCHAR(12)   NOT NULL CHECK (tipo IN ('MESA', 'PARA_LLEVAR')),
  mesa_numero    SMALLINT      REFERENCES mesa (numero),
  cliente_nombre VARCHAR(30),
  estado         VARCHAR(10)   NOT NULL CHECK (estado IN ('ABIERTA', 'PAGADA')),
  total          NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
  abierta_en     TIMESTAMPTZ   NOT NULL,
  pagada_en      TIMESTAMPTZ,
  version        BIGINT        NOT NULL DEFAULT 0,               -- bloqueo optimista (RN-09)
  CHECK ((tipo = 'MESA' AND mesa_numero IS NOT NULL)
      OR (tipo = 'PARA_LLEVAR' AND mesa_numero IS NULL AND cliente_nombre IS NOT NULL)),
  CHECK ((estado = 'ABIERTA' AND pagada_en IS NULL) OR (estado = 'PAGADA' AND pagada_en IS NOT NULL))
);
-- RN-13: una sola cuenta abierta por mesa
CREATE UNIQUE INDEX ux_cuenta_mesa_abierta ON cuenta (mesa_numero) WHERE estado = 'ABIERTA';
CREATE INDEX ix_cuenta_pagada_en ON cuenta (pagada_en) WHERE estado = 'PAGADA';  -- reportes

CREATE TABLE pedido (
  id           BIGSERIAL PRIMARY KEY,
  cuenta_id    BIGINT      NOT NULL REFERENCES cuenta (id),
  creado_por   BIGINT      NOT NULL REFERENCES usuario (id),
  estado       VARCHAR(12) NOT NULL CHECK (estado IN ('PENDIENTE', 'LISTO', 'ENTREGADO', 'ANULADO')),
  creado_en    TIMESTAMPTZ NOT NULL,
  listo_en     TIMESTAMPTZ,
  entregado_en TIMESTAMPTZ
);
CREATE INDEX ix_pedido_cuenta ON pedido (cuenta_id);
CREATE INDEX ix_pedido_estado_creado ON pedido (estado, creado_en);  -- cola de cocina

CREATE TABLE detalle_pedido (
  id               BIGSERIAL PRIMARY KEY,
  pedido_id        BIGINT       NOT NULL REFERENCES pedido (id),
  producto_id      BIGINT       NOT NULL REFERENCES producto (id),
  presentacion_id  BIGINT       REFERENCES presentacion (id),
  cantidad         SMALLINT     NOT NULL CHECK (cantidad > 0),
  precio_unitario  NUMERIC(8,2) NOT NULL CHECK (precio_unitario > 0),  -- precio congelado (RN-02)
  nota             VARCHAR(80),
  anulado          BOOLEAN      NOT NULL DEFAULT FALSE,
  motivo_anulacion VARCHAR(120),
  anulado_por      BIGINT       REFERENCES usuario (id),
  anulado_en       TIMESTAMPTZ,
  CHECK (anulado = FALSE OR (motivo_anulacion IS NOT NULL AND anulado_por IS NOT NULL AND anulado_en IS NOT NULL))  -- RN-10
);
CREATE INDEX ix_detalle_pedido ON detalle_pedido (pedido_id);
CREATE INDEX ix_detalle_producto ON detalle_pedido (producto_id);

CREATE TABLE detalle_sin_acompanamiento (
  detalle_id        BIGINT NOT NULL REFERENCES detalle_pedido (id),
  acompanamiento_id BIGINT NOT NULL REFERENCES acompanamiento (id),
  PRIMARY KEY (detalle_id, acompanamiento_id)
);

CREATE TABLE pago (
  id            BIGSERIAL PRIMARY KEY,
  cuenta_id     BIGINT        NOT NULL REFERENCES cuenta (id),
  caja_id       BIGINT        NOT NULL REFERENCES caja (id),
  metodo        VARCHAR(10)   NOT NULL CHECK (metodo IN ('EFECTIVO', 'YAPE')),
  monto         NUMERIC(10,2) NOT NULL CHECK (monto > 0),
  recibido      NUMERIC(10,2),
  vuelto        NUMERIC(10,2) CHECK (vuelto >= 0),
  nro_operacion VARCHAR(20),
  registrado_en TIMESTAMPTZ   NOT NULL,
  CHECK (metodo = 'YAPE' OR (recibido IS NOT NULL AND recibido >= monto))  -- RN-08
);
CREATE INDEX ix_pago_cuenta ON pago (cuenta_id);
CREATE INDEX ix_pago_caja ON pago (caja_id);

CREATE TABLE gasto (
  id        BIGSERIAL PRIMARY KEY,
  tipo      VARCHAR(6)    NOT NULL CHECK (tipo IN ('DIARIO', 'FIJO')),
  fecha     DATE,
  mes       CHAR(7)       NOT NULL CHECK (mes ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),  -- YYYY-MM
  categoria VARCHAR(30)   NOT NULL,
  monto     NUMERIC(10,2) NOT NULL CHECK (monto > 0),
  nota      VARCHAR(120),
  CHECK (tipo = 'FIJO' OR fecha IS NOT NULL)
);
CREATE INDEX ix_gasto_mes ON gasto (mes);

CREATE TABLE insumo (
  id     BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL UNIQUE,
  orden  SMALLINT    NOT NULL,
  activo BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE aviso_insumo (
  id            BIGSERIAL PRIMARY KEY,
  insumo_id     BIGINT      NOT NULL REFERENCES insumo (id),
  creado_por    BIGINT      NOT NULL REFERENCES usuario (id),
  prioridad     VARCHAR(8)  NOT NULL CHECK (prioridad IN ('NORMAL', 'URGENTE')),
  estado        VARCHAR(12) NOT NULL CHECK (estado IN ('PENDIENTE', 'PREPARANDO', 'LISTO')),
  creado_en     TIMESTAMPTZ NOT NULL,
  preparando_en TIMESTAMPTZ,
  listo_en      TIMESTAMPTZ
);
-- RN-15: un solo aviso no terminado por insumo
CREATE UNIQUE INDEX ux_aviso_pendiente ON aviso_insumo (insumo_id) WHERE estado <> 'LISTO';
