-- HU-05: los acompanamientos se buscan y crean por nombre sin importar mayusculas ("Camote" = "camote").
-- Igual que V3 para producto: el indice evita duplicados por carrera y sirve a la busqueda por lower(nombre).
CREATE UNIQUE INDEX ux_acompanamiento_nombre_ci ON acompanamiento (lower(nombre));
