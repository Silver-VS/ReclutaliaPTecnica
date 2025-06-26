/*
 * File: 04_sp_update_employee.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Actualiza los datos de un empleado existente y devuelve el número de filas afectadas.
 *
 * Parameters:
 *   IN  p_id            INT          -- ID del empleado a modificar
 *   IN  p_first_name    VARCHAR(50)  -- Nuevo nombre
 *   IN  p_last_name     VARCHAR(50)  -- Nuevo apellido
 *   IN  p_email         VARCHAR(100) -- Nuevo correo electrónico
 *   IN  p_salary        DECIMAL(15,2)-- Nuevo salario
 *   OUT p_rows_affected INT          -- Cantidad de filas modificadas
 */
DELIMITER //
CREATE PROCEDURE sp_update_employee(
    IN p_id          INT,
    IN p_first_name  VARCHAR(50),
    IN p_last_name   VARCHAR(50),
    IN p_email       VARCHAR(100),
    IN p_salary      DECIMAL(15,2),
    OUT p_rows_affected INT
)
BEGIN
UPDATE employees
SET
    first_name = p_first_name,
    last_name  = p_last_name,
    email      = p_email,
    salary     = p_salary
WHERE id = p_id;

SET p_rows_affected = ROW_COUNT();
END //
DELIMITER ;
