USE `safari`;

CREATE TABLE IF NOT EXISTS `usuario_roles` (
  `usuario_id` INT NOT NULL,
  `rol_id` INT NOT NULL,
  PRIMARY KEY (`usuario_id`, `rol_id`),
  CONSTRAINT `fk_usuario_roles_usuario`
    FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuario_roles_rol`
    FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `usuario_roles` (`usuario_id`, `rol_id`)
SELECT `id`, `id_rol`
FROM `usuarios`
WHERE `id_rol` IS NOT NULL;

ALTER TABLE `usuarios`
  DROP FOREIGN KEY `fk_usuarios_rol`,
  DROP COLUMN `id_rol`;
