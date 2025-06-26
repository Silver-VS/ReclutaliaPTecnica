/*
 * File: 05_sp_delete_employee.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Elimina un empleado por ID y devuelve el número de filas afectadas.
 *
 * Parameters:
 *   IN  p_id            INT -- ID del empleado a eliminar
 *   OUT p_rows_affected INT -- Cantidad de filas eliminadas
 */
DELIMITER //
CREATE PROCEDURE sp_delete_employee(
    IN p_id INT,
    OUT p_rows_affected INT
)
BEGIN
DELETE FROM employees
WHERE id = p_id;

SET p_rows_affected = ROW_COUNT();
END //
DELIMITER ;
