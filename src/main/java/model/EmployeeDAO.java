/*
 * EmployeeDAO.java
 * ---------------------------------------------------------------------------
 * Copyright © 2025 Silver VS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * ---------------------------------------------------------------------------
 */

package model;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para operaciones CRUD de empleados mediante stored procedures.
 * <p>
 * Todas las implementaciones deben usar {@code CallableStatement} para invocar los SP.
 *
 * @author Silver VS
 * @version 1.0
 * @since 2025-06-26
 */
public interface EmployeeDAO {

    /**
     * Inserta un nuevo empleado en la base de datos.
     *
     * @param employee objeto con los datos del empleado (sin ID ni timestamps)
     * @return ID generado para el nuevo empleado
     */
    int createEmployee(Employee employee);

    /**
     * Recupera un empleado por su identificador.
     *
     * @param id identificador único del empleado
     * @return {@code Optional<Employee>} con el empleado, o vacío si no existe
     */
    Optional<Employee> getEmployeeById(int id);

    /**
     * Obtiene la lista completa de empleados.
     *
     * @return lista ordenada de empleados
     */
    List<Employee> getAllEmployees();

    /**
     * Actualiza los datos de un empleado existente.
     *
     * @param employee objeto con ID y nuevos datos de campos modificables
     * @return {@code true} si la actualización afectó al menos una fila
     */
    boolean updateEmployee(Employee employee);

    /**
     * Elimina un empleado por su identificador.
     *
     * @param id identificador único del empleado a borrar
     * @return {@code true} si al menos una fila fue eliminada
     */
    boolean deleteEmployee(int id);
}
