package service;

import model.Employee;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz que define las operaciones de negocio para la gestión de empleados.
 * <p>
 * Ofrece métodos CRUD y cálculo de los 10 salarios más altos a partir de
 * datos persistidos y potencialmente combinados con fuentes externas.
 * </p>
 *
 * @author Silver VS
 * @version 1.1
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
     * @return {@code Optional<Employee>} con el empleado si existe, o vacío
     */
    Optional<Employee> getEmployeeById(int id);

    /**
     * Obtiene la lista completa de empleados.
     *
     * @return lista de empleados ordenada por ID ascendente
     */
    List<Employee> findAll();

    /**
     * Actualiza los datos de un empleado existente tras validación.
     *
     * @param employee objeto con ID y nuevos valores
     * @return {@code true} si la actualización tuvo éxito, {@code false} si no existe
     * @throws IllegalArgumentException si los datos no son válidos
     */
    boolean update(Employee employee);

    /**
     * Elimina un empleado por su identificador.
     *
     * @param id identificador único
     * @return {@code true} si la eliminación tuvo éxito, {@code false} si no existe
     */
    boolean delete(int id);

    /**
     * Calcula los 10 salarios más altos combinando datos de la base de datos
     * y/o fuentes externas.
     *
     * @return lista de los 10 salarios más altos en orden descendente
     */
    List<Employee> top10EmployeesBySalary();
}
