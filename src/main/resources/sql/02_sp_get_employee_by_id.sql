/*
 * File: 02_sp_get_employee_by_id.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Obtiene todos los datos de un empleado dado su ID.
 *
 * Parameters:
 *   IN p_id INT -- ID del empleado a recuperar
 */
DROP PROCEDURE IF EXISTS sp_get_employee_by_id;
CREATE PROCEDURE sp_get_employee_by_id(
    IN p_id INT
)
BEGIN
    SELECT
        id,
        name,
        position,
        salary,
        hire_date,
        department
    FROM employees
    WHERE id = p_id;
END;
