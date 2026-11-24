-- Datos iniciales: mesas, carta, acompanamientos e insumos avisables.
-- No se crean usuarios aqui: el primer ADMIN se crea con HU-01 desde variables de entorno (nunca un PIN en el repositorio).

INSERT INTO mesa (numero)
SELECT generate_series(1, 14);

INSERT INTO producto (nombre, categoria, precio, requiere_cocina) VALUES
  ('Ceviche de pescado',    'PLATO',  15.00, TRUE),
  ('Arroz con mariscos',    'PLATO',  12.00, TRUE),
  ('Chaufa de mariscos',    'PLATO',  15.00, TRUE),
  ('Chilcano',              'PLATO',   6.00, TRUE),
  ('Parihuela',             'PLATO',   7.00, TRUE),
  ('Porción de chicharrón', 'PLATO',  10.00, TRUE),
  ('Inca Kola',             'BEBIDA', NULL,  FALSE),
  ('Coca-Cola',             'BEBIDA', NULL,  FALSE),
  ('Táper',                 'TAPER',   1.00, FALSE);

INSERT INTO presentacion (producto_id, nombre, precio)
SELECT p.id, v.nombre, v.precio
FROM producto p
CROSS JOIN (VALUES ('Personal', 2.00), ('Gordita', 4.00), ('1 L', 6.00), ('1.5 L', 9.50)) AS v (nombre, precio)
WHERE p.nombre IN ('Inca Kola', 'Coca-Cola');

INSERT INTO acompanamiento (nombre) VALUES
  ('Camote'), ('Cebolla'), ('Lechuga'), ('Cancha'), ('Fideos'),
  ('Chicharrón'), ('Pescado'), ('Porción de ceviche'), ('Jugo de ceviche');

INSERT INTO producto_acompanamiento (producto_id, acompanamiento_id)
SELECT p.id, a.id
FROM (VALUES
  ('Ceviche de pescado',    'Chicharrón'), ('Ceviche de pescado', 'Cebolla'),
  ('Ceviche de pescado',    'Camote'),     ('Ceviche de pescado', 'Lechuga'),
  ('Arroz con mariscos',    'Porción de ceviche'), ('Arroz con mariscos', 'Chicharrón'),
  ('Arroz con mariscos',    'Cebolla'),    ('Arroz con mariscos', 'Cancha'),
  ('Arroz con mariscos',    'Camote'),     ('Arroz con mariscos', 'Lechuga'),
  ('Chilcano',              'Camote'),     ('Chilcano', 'Cebolla'),
  ('Chilcano',              'Pescado'),    ('Chilcano', 'Chicharrón'),
  ('Chilcano',              'Cancha'),     ('Chilcano', 'Fideos'),
  ('Parihuela',             'Fideos'),
  ('Porción de chicharrón', 'Jugo de ceviche'), ('Porción de chicharrón', 'Camote'),
  ('Porción de chicharrón', 'Cancha'),     ('Porción de chicharrón', 'Cebolla'),
  ('Porción de chicharrón', 'Lechuga')
) AS m (producto, acompanamiento)
JOIN producto p ON p.nombre = m.producto
JOIN acompanamiento a ON a.nombre = m.acompanamiento;

INSERT INTO insumo (nombre, orden) VALUES
  ('Ceviche', 1), ('Chicharrón', 2), ('Camote', 3), ('Cebolla', 4), ('Lechuga', 5),
  ('Cancha', 6), ('Fideos', 7), ('Arroz con mariscos', 8), ('Chaufa', 9), ('Parihuela', 10);
