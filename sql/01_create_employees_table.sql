/*
 * File: 01_create_employees_table.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Define la estructura de la tabla `employees`, incluyendo claves,
 *   restricciones de unicidad y auditoría de tiempos de creación y actualización.
 */
CREATE TABLE employees (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           first_name VARCHAR(50)  NOT NULL COMMENT 'Nombre del empleado',
                           last_name  VARCHAR(50)  NOT NULL COMMENT 'Apellido del empleado',
                           email      VARCHAR(100) NOT NULL UNIQUE COMMENT 'Correo electrónico (único)',
                           salary     DECIMAL(15,2) NOT NULL COMMENT 'Salario del empleado',
                           created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
                               COMMENT 'Fecha y hora de creación del registro',
                           updated_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP
    COMMENT 'Fecha y hora de la última actualización'
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
COMMENT='Tabla de empleados para gestión CRUD';
