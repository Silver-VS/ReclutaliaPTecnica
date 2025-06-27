/*
 * EmployeeService.java
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

package service;

import model.Employee;
import model.EmployeeDAO;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Interfaz que define las operaciones de negocio para la gestión de empleados.
 * <p>
 * Ofrece métodos CRUD y cálculo de los 10 salarios más altos a partir de archivos JSON externos.
 * </p>
 *
 * @author Silver VS
 * @version 1.0
 * @since 2025-06-27
 */
public interface EmployeeService {

    /**
     * Crea un nuevo empleado tras validar sus datos.
     *
     * @param employee datos del empleado (sin ID)
     * @return ID generado para el nuevo empleado
     * @throws IllegalArgumentException si los datos del empleado no son válidos
     */
    int create(Employee employee);

    /**
     * Recupera un empleado por su identificador.
     *
     * @param id identificador único
     * @return {@code Optional<Employee>} con el empleado si existe
     */
    Optional<Employee> findById(int id);

    /**
     * Obtiene la lista completa de empleados.
     *
     * @return lista ordenada de empleados
     */
    List<Employee> findAll();

    /**
     * Actualiza los datos de un empleado existente tras validación.
     *
     * @param employee objeto con ID y nuevos valores
     * @return {@code true} si la actualización tuvo éxito
     * @throws IllegalArgumentException si los datos no son válidos
     */
    boolean update(Employee employee);

    /**
     * Elimina un empleado por su identificador.
     *
     * @param id identificador único
     * @return {@code true} si la eliminación tuvo éxito
     */
    boolean delete(int id);

    /**
     * Calcula los 10 salarios más altos combinando datos de tres archivos JSON externos.
     *
     * @return lista de los 10 salarios más altos en orden descendente
     */
    List<BigDecimal> top10Salaries();
}

