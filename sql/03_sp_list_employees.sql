/*
 * File: 03_sp_list_employees.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Devuelve la lista completa de empleados ordenada por ID.
 *
 * Parameters:
 *   Ninguno
 *
 * Result:
 *   Conjunto de resultados con las columnas:
 *     id, first_name, last_name, email, salary, created_at, updated_at
 */
DELIMITER //
CREATE PROCEDURE sp_list_employees()
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
ORDER BY id;
END //
DELIMITER ;
