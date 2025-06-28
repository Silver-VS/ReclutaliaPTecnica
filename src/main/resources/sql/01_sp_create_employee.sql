/*
 * File: 01_sp_create_employee.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Inserta un nuevo registro en `employees` y devuelve el ID generado.
 *
 * Parameters:
 *   IN  p_name       VARCHAR(100)  -- Nombre completo del empleado
 *   IN  p_position   VARCHAR(100)  -- Puesto de trabajo
 *   IN  p_salary     DECIMAL(15,2) -- Salario del empleado
 *   IN  p_hire_date  DATE          -- Fecha de contratación
 *   IN  p_department VARCHAR(100)  -- Departamento del empleado
 *   OUT p_new_id     INT           -- ID del nuevo empleado creado
 */
DROP PROCEDURE IF EXISTS sp_create_employee;
CREATE PROCEDURE sp_create_employee(
    IN  p_name       VARCHAR(100),
    IN  p_position   VARCHAR(100),
    IN  p_salary     DECIMAL(15,2),
    IN  p_hire_date  DATE,
    IN  p_department VARCHAR(100),
    OUT p_new_id     INT
)
BEGIN
    INSERT INTO employees (name, position, salary, hire_date, department)
    VALUES (p_name, p_position, p_salary, p_hire_date, p_department);
    SET p_new_id = LAST_INSERT_ID();
END;
