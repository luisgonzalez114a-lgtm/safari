CREATE DATABASE IF NOT EXISTS `safari`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
USE `safari`;

SET NAMES utf8mb4;

CREATE TABLE `roles` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `modulos` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(60) NOT NULL,
  `descripcion` VARCHAR(255) DEFAULT NULL,
  `icono` VARCHAR(50) DEFAULT NULL,
  `orden` INT DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_modulos_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `vistas` (
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

CREATE TABLE `permisos` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre_permiso` VARCHAR(50) NOT NULL,
  `descripcion` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_permisos_nombre` (`nombre_permiso`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `r_modulo_rol` (
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

CREATE TABLE `vistas_permiso` (
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

CREATE TABLE `usuarios` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL,
  `telefono` VARCHAR(20) DEFAULT NULL,
  `correo` VARCHAR(100) DEFAULT NULL,
  `clave` VARCHAR(255) NOT NULL,
  `estatus` INT DEFAULT 1,
  `fecha_registro` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `usuario_roles` (
  `usuario_id` INT NOT NULL,
  `rol_id` INT NOT NULL,
  PRIMARY KEY (`usuario_id`, `rol_id`),
  CONSTRAINT `fk_usuario_roles_usuario`
    FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuario_roles_rol`
    FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `categorias` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL,
  `descripcion` TEXT DEFAULT NULL,
  `estatus` INT DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `marcas` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL,
  `estatus` INT DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `productos` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `codigo` VARCHAR(50) NOT NULL,
  `nombre` VARCHAR(100) NOT NULL,
  `descripcion` TEXT DEFAULT NULL,
  `id_marca` INT DEFAULT NULL,
  `id_categoria` INT DEFAULT NULL,
  `precio_compra` DECIMAL(18,2) NOT NULL,
  `precio_actual` DECIMAL(18,2) NOT NULL,
  `stock_minimo` INT DEFAULT 0,
  `stock_actual` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_productos_codigo` (`codigo`),
  CONSTRAINT `fk_productos_marca`
    FOREIGN KEY (`id_marca`) REFERENCES `marcas` (`id`),
  CONSTRAINT `fk_productos_categoria`
    FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `proveedores` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL,
  `rif_nit` VARCHAR(50) DEFAULT NULL,
  `telefono` VARCHAR(20) DEFAULT NULL,
  `correo` VARCHAR(100) DEFAULT NULL,
  `direccion` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `entradas` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `id_proveedor` INT NOT NULL,
  `id_usuario` INT NOT NULL,
  `fecha_entrada` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `observaciones` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_entradas_proveedor`
    FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id`),
  CONSTRAINT `fk_entradas_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `detalles_entradas` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `id_entrada` INT NOT NULL,
  `id_producto` INT NOT NULL,
  `cantidad` INT NOT NULL,
  `precio_compra` DECIMAL(18,2) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_detalles_entradas_entrada`
    FOREIGN KEY (`id_entrada`) REFERENCES `entradas` (`id`),
  CONSTRAINT `fk_detalles_entradas_producto`
    FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `salidas` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `id_usuario` INT NOT NULL,
  `fecha_salida` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `monto_total` DECIMAL(18,2) NOT NULL,
  `observaciones` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_salidas_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `detalles_salidas` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `id_salida` INT NOT NULL,
  `id_producto` INT NOT NULL,
  `cantidad` INT NOT NULL,
  `precio_venta` DECIMAL(18,2) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_detalles_salidas_salida`
    FOREIGN KEY (`id_salida`) REFERENCES `salidas` (`id`),
  CONSTRAINT `fk_detalles_salidas_producto`
    FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `auditoria_logs` (
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
