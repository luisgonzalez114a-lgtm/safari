ALTER TABLE `usuarios`
  ADD COLUMN `bono_descuento_disponible` BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE `salidas`
  ADD COLUMN `subtotal` DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN `monto_descuento` DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN `porcentaje_descuento` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN `cliente_usuario_id` INT DEFAULT NULL,
  ADD COLUMN `cliente_nombre` VARCHAR(100) DEFAULT NULL,
  ADD COLUMN `cliente_telefono` VARCHAR(20) DEFAULT NULL,
  ADD COLUMN `cliente_correo` VARCHAR(100) DEFAULT NULL;

UPDATE `salidas`
SET `subtotal` = `monto_total`
WHERE `subtotal` = 0.00 AND `monto_total` <> 0.00;
