/*
 * File: 02_sp_get_employee_by_id.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Recupera todos los datos de un empleado dado su ID.
 *
 * Parameters:
 *   IN p_id INT -- Identificador del empleado a buscar
 *
 * Result:
 *   Conjunto de resultados con las columnas:
 *     id, first_name, last_name, email, salary, created_at, updated_at
 */
DELIMITER //
CREATE PROCEDURE sp_get_employee_by_id(
    IN p_id INT
)
BEGIN
SELECT
    id,
    first_name,
    last_name,
    email,
    salary,
    created_at,
    updated_at
FROM employees
WHERE id = p_id;
END //
DELIMITER ;
