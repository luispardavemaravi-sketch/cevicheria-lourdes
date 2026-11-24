-- HU-04: una presentacion que el ADMIN quita no se borra (detalle_pedido la referencia): queda inactiva.
ALTER TABLE presentacion ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
