/*s
 * File: 01_sp_create_employee.sql
 * Author: Silver VS
 * Date: 26 June 2025
 * Description:
 *   Inserta un nuevo registro en `employees` y devuelve el ID generado.
 *
 * Parameters:
 *   IN  p_first_name  VARCHAR(50)  -- Nombre del empleado
 *   IN  p_last_name   VARCHAR(50)  -- Apellido del empleado
 *   IN  p_email       VARCHAR(100) -- Correo electrónico (único)
 *   IN  p_salary      DECIMAL(15,2)-- Salario del empleado
 *   OUT p_new_id      INT          -- ID del nuevo empleado creado
 */
DELIMITER //
CREATE PROCEDURE sp_create_employee(
    IN  p_first_name  VARCHAR(50),
    IN  p_last_name   VARCHAR(50),
    IN  p_email       VARCHAR(100),
    IN  p_salary      DECIMAL(15,2),
    OUT p_new_id      INT
)
BEGIN
INSERT INTO employees (first_name, last_name, email, salary)
VALUES (p_first_name, p_last_name, p_email, p_salary);
SET p_new_id = LAST_INSERT_ID();
END //
DELIMITER ;
