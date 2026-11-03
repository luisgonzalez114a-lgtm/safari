-- Adds the supplier invoice/document reference to existing SAFARI databases.
-- Safe to run more than once.
SET @referencia_documento_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'entradas'
      AND COLUMN_NAME = 'referencia_documento'
);
SET @referencia_documento_ddl := IF(
    @referencia_documento_exists = 0,
    'ALTER TABLE entradas ADD COLUMN referencia_documento VARCHAR(100) NULL AFTER fecha_entrada',
    'SELECT 1'
);
PREPARE referencia_documento_stmt FROM @referencia_documento_ddl;
EXECUTE referencia_documento_stmt;
DEALLOCATE PREPARE referencia_documento_stmt;
