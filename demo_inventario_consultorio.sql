USE `safari`;

CREATE TEMPORARY TABLE IF NOT EXISTS `_demo_productos_consultorio` (
  `codigo` VARCHAR(50) NOT NULL PRIMARY KEY,
  `nombre` VARCHAR(100) NOT NULL,
  `descripcion` VARCHAR(500) NOT NULL,
  `categoria` VARCHAR(50) NOT NULL,
  `precio_compra` DECIMAL(18,2) NOT NULL,
  `precio_actual` DECIMAL(18,2) NOT NULL,
  `cantidad` INT NOT NULL,
  `stock_minimo` INT NOT NULL DEFAULT 2
);
CREATE TEMPORARY TABLE IF NOT EXISTS `_demo_codigos_nuevos` (
  `codigo` VARCHAR(50) NOT NULL PRIMARY KEY
);
DELETE FROM `_demo_productos_consultorio`;
DELETE FROM `_demo_codigos_nuevos`;

START TRANSACTION;

INSERT INTO categorias (nombre, descripcion, estatus)
SELECT 'Perros', 'Productos para perros.', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Perros');
INSERT INTO categorias (nombre, descripcion, estatus)
SELECT 'Gatos', 'Productos para gatos.', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Gatos');
INSERT INTO categorias (nombre, descripcion, estatus)
SELECT 'Medicinas', 'Medicamentos y productos veterinarios de muestra.', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Medicinas');
INSERT INTO categorias (nombre, descripcion, estatus)
SELECT 'Consultorio', 'Insumos de muestra para consultorio veterinario.', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Consultorio');
INSERT INTO categorias (nombre, descripcion, estatus)
SELECT 'Accesorios', 'Accesorios de muestra para mascotas.', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Accesorios');
INSERT INTO categorias (nombre, descripcion, estatus)
SELECT 'Juguetes', 'Juguetes de muestra para perros y gatos.', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Juguetes');
INSERT INTO categorias (nombre, descripcion, estatus)
SELECT 'Alimentación', 'Alimentos y premios de muestra para mascotas.', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Alimentación');
INSERT INTO categorias (nombre, descripcion, estatus)
SELECT 'Higiene', 'Productos de higiene de muestra para mascotas.', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Higiene');

INSERT INTO proveedores (nombre, rif_nit, telefono, correo, direccion)
SELECT 'DEMO SAFARI - Proveedor ficticio de consultorio', 'DEMO-CONS-2026', '5550101',
       'proveedor.consultorio@safari.test', 'Proveedor ficticio para datos de demostración'
WHERE NOT EXISTS (
  SELECT 1 FROM proveedores
  WHERE nombre = 'DEMO SAFARI - Proveedor ficticio de consultorio'
);

INSERT INTO `_demo_productos_consultorio`
  (codigo, nombre, descripcion, categoria, precio_compra, precio_actual, cantidad, stock_minimo)
VALUES
  ('DEMO26-PER-001', 'Collar nylon ajustable para perro', 'Collar de uso diario para perro.', 'Perros', 4.00, 8.00, 6, 2),
  ('DEMO26-PER-002', 'Correa nylon 1.5 m para perro', 'Correa resistente para paseo de perro.', 'Perros', 5.00, 9.50, 4, 2),
  ('DEMO26-PER-003', 'Plato antideslizante para perro', 'Plato lavable para alimento de perro.', 'Perros', 3.75, 7.25, 3, 2),
  ('DEMO26-PER-004', 'Cortaunas pequeño para perros', 'Cortaúñas doméstico para perros pequeños.', 'Perros', 3.50, 6.50, 5, 2),
  ('DEMO26-PER-005', 'Cepillo desenredante para perros', 'Cepillo de cuidado diario para perros.', 'Perros', 4.75, 8.75, 4, 2),

  ('DEMO26-GAT-001', 'Arena sanitaria gatos 4 kg', 'Arena aglomerante para caja sanitaria de gatos.', 'Gatos', 5.20, 8.50, 6, 2),
  ('DEMO26-GAT-002', 'Ratón de tela con catnip para gatos', 'Juguete pequeño con catnip para gatos.', 'Gatos', 1.50, 3.50, 8, 2),
  ('DEMO26-GAT-003', 'Rascador de cartón para gatos', 'Rascador compacto para gatos de interior.', 'Gatos', 7.00, 12.00, 2, 2),
  ('DEMO26-GAT-004', 'Plato cerámico para gatos', 'Plato lavable para agua o alimento de gatos.', 'Gatos', 3.50, 7.00, 4, 2),
  ('DEMO26-GAT-005', 'Collar con cascabel para gatos', 'Collar ajustable con cascabel para gatos.', 'Gatos', 2.80, 5.50, 3, 2),

  ('DEMO26-MED-001', 'Antiparasitario oral para perros 2-10 kg', 'Antiparasitario veterinario de muestra para perros; confirmar indicación profesional.', 'Medicinas', 5.50, 8.50, 3, 2),
  ('DEMO26-MED-002', 'Pipeta antipulgas para gatos', 'Pipeta veterinaria de muestra para gatos; elegir presentación según indicación profesional.', 'Medicinas', 5.00, 9.00, 4, 2),
  ('DEMO26-MED-003', 'Desparasitante oral para mascotas', 'Desparasitante veterinario de muestra; uso según etiqueta e indicación profesional.', 'Medicinas', 3.00, 5.50, 5, 2),
  ('DEMO26-MED-004', 'Champú medicado para mascotas 250 ml', 'Champú veterinario de muestra para perros y gatos; consultar etiqueta.', 'Medicinas', 6.50, 11.00, 3, 2),
  ('DEMO26-MED-005', 'Suplemento vitamínico para perros', 'Suplemento veterinario de muestra para perros; consultar indicación profesional.', 'Medicinas', 5.75, 10.00, 4, 2),

  ('DEMO26-CON-001', 'Guantes de nitrilo para consultorio caja x100', 'Guantes desechables de nitrilo para consultorio veterinario.', 'Consultorio', 5.50, 9.50, 2, 2),
  ('DEMO26-CON-002', 'Jeringas descartables 3 ml paquete x10', 'Jeringas desechables para uso en consultorio veterinario.', 'Consultorio', 1.25, 3.00, 8, 2),
  ('DEMO26-CON-003', 'Gasas estériles paquete x10', 'Gasas para curaciones en consultorio veterinario.', 'Consultorio', 2.00, 4.00, 6, 2),
  ('DEMO26-CON-004', 'Termómetro digital veterinario', 'Termómetro digital para consultorio veterinario.', 'Consultorio', 7.00, 12.00, 2, 2),
  ('DEMO26-CON-005', 'Venda cohesiva para consultorio 5 cm', 'Venda de soporte para consultorio veterinario.', 'Consultorio', 2.00, 4.00, 5, 2),

  ('DEMO26-ACC-001', 'Pechera acolchada para perro mediano', 'Pechera ajustable de paseo para perro.', 'Accesorios', 9.00, 15.00, 3, 2),
  ('DEMO26-ACC-002', 'Transportadora pequeña para mascotas', 'Transportadora doméstica pequeña para perro o gato.', 'Accesorios', 22.00, 35.00, 2, 2),
  ('DEMO26-ACC-003', 'Bebedero portátil para perro', 'Bebedero de viaje reutilizable para perro.', 'Accesorios', 5.50, 10.00, 4, 2),
  ('DEMO26-ACC-004', 'Cama pequeña para perro o gato', 'Cama lavable de tamaño pequeño para mascotas.', 'Accesorios', 18.00, 28.00, 2, 2),
  ('DEMO26-ACC-005', 'Placa de identificación para mascota', 'Placa liviana de identificación para collar de mascota.', 'Accesorios', 2.50, 6.00, 5, 2),

  ('DEMO26-JUG-001', 'Pelota de goma para perros', 'Juguete lavable para juego supervisado con perros.', 'Juguetes', 2.50, 6.00, 6, 2),
  ('DEMO26-JUG-002', 'Cuerda mordedor para perro', 'Juguete de cuerda para perros.', 'Juguetes', 3.50, 7.50, 5, 2),
  ('DEMO26-JUG-003', 'Juguete plumero para gatos', 'Juguete interactivo tipo plumero para gatos.', 'Juguetes', 2.00, 5.50, 3, 2),
  ('DEMO26-JUG-004', 'Túnel plegable para gatos', 'Túnel de juego plegable para gatos de interior.', 'Juguetes', 10.00, 18.00, 2, 2),
  ('DEMO26-JUG-005', 'Dispensador de premios interactivo para mascotas', 'Juguete dispensador de premios para perros y gatos.', 'Juguetes', 7.00, 12.00, 7, 2),

  ('DEMO26-ALI-001', 'Alimento seco perro adulto 1 kg', 'Alimento balanceado de muestra para perro adulto.', 'Alimentación', 5.50, 7.50, 6, 2),
  ('DEMO26-ALI-002', 'Alimento seco gato adulto 1 kg', 'Alimento balanceado de muestra para gato adulto.', 'Alimentación', 6.00, 8.50, 5, 2),
  ('DEMO26-ALI-003', 'Snack dental para perros paquete', 'Premios masticables de muestra para perros.', 'Alimentación', 2.50, 4.50, 8, 2),
  ('DEMO26-ALI-004', 'Alimento húmedo para gatos 85 g', 'Alimento húmedo de muestra para gatos.', 'Alimentación', 1.00, 1.80, 8, 2),
  ('DEMO26-ALI-005', 'Galletas para perro bolsa pequeña', 'Premios horneados de muestra para perros.', 'Alimentación', 2.20, 4.00, 5, 2),

  ('DEMO26-HIG-001', 'Champú suave para mascotas 500 ml', 'Champú de higiene regular para perros y gatos.', 'Higiene', 4.50, 8.50, 4, 2),
  ('DEMO26-HIG-002', 'Toallitas húmedas para mascotas paquete x40', 'Toallitas para limpieza externa de perros y gatos.', 'Higiene', 2.50, 4.50, 6, 2),
  ('DEMO26-HIG-003', 'Cepillo dental para perros', 'Cepillo para higiene dental doméstica de perros.', 'Higiene', 1.75, 4.00, 5, 2),
  ('DEMO26-HIG-004', 'Pasta dental para perro 100 g', 'Pasta dental de uso veterinario para perros.', 'Higiene', 3.00, 5.50, 3, 2),
  ('DEMO26-HIG-005', 'Limpiador de oídos para perros y gatos', 'Limpiador externo de muestra para higiene de oídos de mascotas.', 'Higiene', 5.00, 9.00, 4, 2);

INSERT INTO `_demo_codigos_nuevos` (codigo)
SELECT demo.codigo
FROM `_demo_productos_consultorio` demo
WHERE NOT EXISTS (SELECT 1 FROM productos p WHERE p.codigo = demo.codigo);

INSERT INTO productos
  (codigo, nombre, descripcion, id_categoria, precio_compra, precio_actual, stock_minimo, stock_actual)
SELECT demo.codigo, demo.nombre, demo.descripcion, categoria.id, 0.00,
       demo.precio_actual, demo.stock_minimo, 0
FROM `_demo_productos_consultorio` demo
JOIN categorias categoria ON categoria.nombre = demo.categoria
WHERE NOT EXISTS (SELECT 1 FROM productos p WHERE p.codigo = demo.codigo);

SET @proveedor_demo_id = (
  SELECT MIN(id) FROM proveedores
  WHERE nombre = 'DEMO SAFARI - Proveedor ficticio de consultorio'
);
SET @usuario_demo_id = (
  SELECT MIN(u.id)
  FROM usuarios u
  JOIN usuario_roles ur ON ur.usuario_id = u.id
  JOIN roles r ON r.id = ur.rol_id
  WHERE r.nombre = 'ADMIN' AND u.estatus = 1
);

INSERT INTO entradas (id_proveedor, id_usuario, fecha_entrada, observaciones)
SELECT @proveedor_demo_id, @usuario_demo_id, CURRENT_TIMESTAMP,
       'DEMO: inventario pequeño de consultorio; productos y proveedor ficticios.'
WHERE EXISTS (SELECT 1 FROM `_demo_codigos_nuevos`);
SET @entrada_demo_id = LAST_INSERT_ID();

INSERT INTO detalles_entradas (id_entrada, id_producto, cantidad, precio_compra)
SELECT @entrada_demo_id, producto.id, demo.cantidad, demo.precio_compra
FROM `_demo_productos_consultorio` demo
JOIN `_demo_codigos_nuevos` nuevo ON nuevo.codigo = demo.codigo
JOIN productos producto ON producto.codigo = demo.codigo;

UPDATE productos producto
JOIN `_demo_productos_consultorio` demo ON demo.codigo = producto.codigo
JOIN `_demo_codigos_nuevos` nuevo ON nuevo.codigo = demo.codigo
SET producto.stock_actual = producto.stock_actual + demo.cantidad,
    producto.precio_compra = demo.precio_compra;

COMMIT;

SELECT categoria.nombre AS categoria, COUNT(producto.id) AS productos_demo,
       SUM(producto.stock_actual) AS unidades_demo
FROM categorias categoria
LEFT JOIN productos producto
  ON producto.id_categoria = categoria.id
  AND producto.codigo LIKE 'DEMO26-%'
GROUP BY categoria.id, categoria.nombre
ORDER BY categoria.nombre;
