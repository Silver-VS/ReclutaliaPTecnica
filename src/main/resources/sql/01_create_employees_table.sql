/*
 * File: 01_create_employees_table.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Define la estructura de la tabla `employees`, incluyendo claves,
 *   restricciones de unicidad y auditoría de tiempos de creación y actualización.
 */
DROP TABLE IF EXISTS employees;

CREATE TABLE employees
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)   NOT NULL,
    position   VARCHAR(100)   NOT NULL,
    salary     DECIMAL(15, 2) NOT NULL,
    hire_date  DATE           NOT NULL,
    department VARCHAR(100)   NOT NULL
);