/*
 * File: 04_sp_update_employee.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Actualiza los datos de un empleado existente.
 *
 * Parameters:
 *   IN p_id         INT           -- ID del empleado a actualizar
 *   IN p_name       VARCHAR(100)  -- Nuevo nombre completo
 *   IN p_position   VARCHAR(100)  -- Nuevo puesto de trabajo
 *   IN p_salary     DECIMAL(15,2) -- Nuevo salario
 *   IN p_hire_date  DATE          -- Nueva fecha de contratación
 *   IN p_department VARCHAR(100)  -- Nuevo departamento
 */
DROP PROCEDURE IF EXISTS sp_update_employee;
CREATE PROCEDURE sp_update_employee(
    IN p_id         INT,
    IN p_name       VARCHAR(100),
    IN p_position   VARCHAR(100),
    IN p_salary     DECIMAL(15,2),
    IN p_hire_date  DATE,
    IN p_department VARCHAR(100)
)
BEGIN
    UPDATE employees
    SET name       = p_name,
        position   = p_position,
        salary     = p_salary,
        hire_date  = p_hire_date,
        department = p_department
    WHERE id = p_id;
END;
