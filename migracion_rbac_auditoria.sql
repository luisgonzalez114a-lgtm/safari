USE `safari`;

CREATE TABLE IF NOT EXISTS `modulos` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(60) NOT NULL,
  `descripcion` VARCHAR(255) DEFAULT NULL,
  `icono` VARCHAR(50) DEFAULT NULL,
  `orden` INT DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_modulos_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `vistas` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(80) NOT NULL,
  `ruta_acceso` VARCHAR(120) NOT NULL,
  `descripcion` VARCHAR(255) DEFAULT NULL,
  `modulo_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_vistas_ruta_acceso` (`ruta_acceso`),
  CONSTRAINT `fk_vistas_modulo`
    FOREIGN KEY (`modulo_id`) REFERENCES `modulos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `permisos` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre_permiso` VARCHAR(50) NOT NULL,
  `descripcion` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_permisos_nombre` (`nombre_permiso`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `r_modulo_rol` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `rol_id` INT NOT NULL,
  `modulo_id` INT NOT NULL,
  `habilitado` BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_r_modulo_rol` (`rol_id`, `modulo_id`),
  CONSTRAINT `fk_r_modulo_rol_rol`
    FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_r_modulo_rol_modulo`
    FOREIGN KEY (`modulo_id`) REFERENCES `modulos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `vistas_permiso` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `r_modulo_rol_id` INT NOT NULL,
  `vista_id` INT NOT NULL,
  `permiso_id` INT NOT NULL,
  `permitido` BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_vistas_permiso` (`r_modulo_rol_id`, `vista_id`, `permiso_id`),
  CONSTRAINT `fk_vistas_permiso_modulo_rol`
    FOREIGN KEY (`r_modulo_rol_id`) REFERENCES `r_modulo_rol` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vistas_permiso_vista`
    FOREIGN KEY (`vista_id`) REFERENCES `vistas` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vistas_permiso_permiso`
    FOREIGN KEY (`permiso_id`) REFERENCES `permisos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `auditoria_logs` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `usuario_id` INT DEFAULT NULL,
  `accion` VARCHAR(100) NOT NULL,
  `tabla_afectada` VARCHAR(50) NOT NULL,
  `registro_id` INT DEFAULT NULL,
  `fecha_hora` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_auditoria_logs_fecha_hora` (`fecha_hora`),
  CONSTRAINT `fk_auditoria_logs_usuario`
    FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `modulos` (`nombre`, `descripcion`, `icono`, `orden`) VALUES
  ('Inventario', 'Productos y existencias', 'box', 1),
  ('Operaciones', 'Entradas y ventas', 'arrows-exchange', 2),
  ('Catálogos', 'Proveedores, marcas y categorías', 'tags', 3),
  ('Seguridad', 'Usuarios, roles y auditoría', 'shield-lock', 4);

INSERT IGNORE INTO `permisos` (`nombre_permiso`, `descripcion`) VALUES
  ('CONSULTAR', 'Consultar la vista y sus datos.'),
  ('CREAR', 'Crear registros desde la vista.'),
  ('EDITAR', 'Modificar registros de la vista.'),
  ('ELIMINAR', 'Eliminar registros de la vista.');

INSERT IGNORE INTO `vistas` (`nombre`, `ruta_acceso`, `descripcion`, `modulo_id`)
SELECT defs.nombre, defs.ruta_acceso, defs.descripcion, m.id
FROM (
  SELECT 'Inventario' nombre, '/inventario' ruta_acceso, 'Consulta y administración de productos.' descripcion, 'Inventario' modulo
  UNION ALL SELECT 'Entradas', '/entradas', 'Recepciones de mercancía.', 'Operaciones'
  UNION ALL SELECT 'Ventas y salidas', '/salidas', 'Ventas y movimientos de salida.', 'Operaciones'
  UNION ALL SELECT 'Proveedores', '/catalogos/proveedores', 'Catálogo de proveedores.', 'Catálogos'
  UNION ALL SELECT 'Marcas', '/catalogos/marcas', 'Catálogo de marcas.', 'Catálogos'
  UNION ALL SELECT 'Categorías', '/catalogos/categorias', 'Catálogo de categorías.', 'Catálogos'
  UNION ALL SELECT 'Usuarios', '/usuarios', 'Administración de cuentas.', 'Seguridad'
  UNION ALL SELECT 'Roles', '/roles', 'Catálogo de roles.', 'Seguridad'
  UNION ALL SELECT 'Permisos', '/roles/permisos', 'Matriz de permisos por módulo y vista.', 'Seguridad'
  UNION ALL SELECT 'Auditoría', '/auditoria', 'Historial de acciones del sistema.', 'Seguridad'
) defs
JOIN `modulos` m ON m.nombre = defs.modulo;

INSERT IGNORE INTO `r_modulo_rol` (`rol_id`, `modulo_id`, `habilitado`)
SELECT r.id, m.id, TRUE
FROM `roles` r
JOIN `modulos` m
WHERE UPPER(r.nombre) = 'ADMIN';

INSERT IGNORE INTO `r_modulo_rol` (`rol_id`, `modulo_id`, `habilitado`)
SELECT r.id, m.id, TRUE
FROM `roles` r
JOIN `modulos` m ON m.nombre = 'Operaciones'
WHERE UPPER(r.nombre) = 'VENDEDOR';

INSERT IGNORE INTO `r_modulo_rol` (`rol_id`, `modulo_id`, `habilitado`)
SELECT r.id, m.id, TRUE
FROM `roles` r
JOIN `modulos` m ON m.nombre = 'Inventario'
WHERE UPPER(r.nombre) IN ('USER', 'VENDEDOR', 'EDITOR');

INSERT IGNORE INTO `vistas_permiso` (`r_modulo_rol_id`, `vista_id`, `permiso_id`, `permitido`)
SELECT rm.id, v.id, p.id, TRUE
FROM `r_modulo_rol` rm
JOIN `roles` r ON r.id = rm.rol_id
JOIN `vistas` v ON v.modulo_id = rm.modulo_id
JOIN `permisos` p
WHERE UPPER(r.nombre) = 'ADMIN';

INSERT IGNORE INTO `vistas_permiso` (`r_modulo_rol_id`, `vista_id`, `permiso_id`, `permitido`)
SELECT rm.id, v.id, p.id, TRUE
FROM `r_modulo_rol` rm
JOIN `roles` r ON r.id = rm.rol_id
JOIN `vistas` v ON v.modulo_id = rm.modulo_id
JOIN `permisos` p
WHERE (UPPER(r.nombre) = 'VENDEDOR' AND
       ((v.ruta_acceso = '/inventario' AND p.nombre_permiso = 'CONSULTAR')
        OR (v.ruta_acceso IN ('/entradas', '/salidas')
            AND p.nombre_permiso IN ('CONSULTAR', 'CREAR'))))
   OR (UPPER(r.nombre) = 'USER' AND v.ruta_acceso = '/inventario'
       AND p.nombre_permiso = 'CONSULTAR')
   OR (UPPER(r.nombre) = 'EDITOR' AND v.ruta_acceso = '/inventario'
       AND p.nombre_permiso IN ('CONSULTAR', 'CREAR', 'EDITAR'));
