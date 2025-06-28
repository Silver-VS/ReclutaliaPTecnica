/*
 * File: 05_sp_delete_employee.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Elimina un empleado de la tabla `employees` dado su ID.
 *
 * Parameters:
 *   IN p_id INT -- ID del empleado a eliminar
 */
DROP PROCEDURE IF EXISTS sp_delete_employee;
CREATE PROCEDURE sp_delete_employee(
    IN p_id INT
)
BEGIN
    DELETE FROM employees
    WHERE id = p_id;
END;
