/*
 * File: 03_sp_list_employees.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Lista todos los empleados registrados en la tabla `employees`.
 *
 * Parameters:
 *   (ninguno)
 */
DROP PROCEDURE IF EXISTS sp_list_employees;
CREATE PROCEDURE sp_list_employees()
BEGIN
    SELECT
        id,
        name,
        position,
        salary,
        hire_date,
        department
    FROM employees;
END;
