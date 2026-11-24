-- HU-03: el nombre de producto es unico sin importar mayusculas ("Ceviche" = "ceviche").
-- El caso de uso ya lo verifica, pero este indice cierra la carrera entre dos altas simultaneas
-- y permite que la busqueda por lower(nombre) use indice.
CREATE UNIQUE INDEX ux_producto_nombre_ci ON producto (lower(nombre));
