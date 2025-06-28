package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.Employee;
import model.EmployeeDAO;
import model.EmployeeDAOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementación de {@link EmployeeService} que aplica reglas de negocio
 * y orquesta operaciones CRUD mediante {@link EmployeeDAO}.
 * <p>
 * Valida datos antes de delegar al DAO y combina múltiples fuentes JSON
 * para calcular el top 10 de salarios.
 * </p>
 *
 * @author Silver VS
 * @version 1.1
 * @since 2025-06-27
 */
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeServiceImpl.class);
    private final EmployeeDAOImpl dao;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);;

    /**
     * Construye el servicio con la implementación de DAO proporcionada.
     *
     * @param dao instancia de acceso a datos
     */
    public EmployeeServiceImpl(EmployeeDAOImpl dao) {
        this.dao = dao;
    }

    @Override
    public int create(Employee employee) {
        validateEmployee(employee, false);
        LOG.info("Creando empleado: {}", employee.getName());
        return dao.createEmployee(employee);
    }

    @Override
    public Optional<Employee> getEmployeeById(int id) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido");
        return dao.getEmployeeById(id);
    }

    @Override
    public List<Employee> findAll() {
        return dao.getAllEmployees();
    }

    @Override
    public boolean update(Employee employee) {
        if (employee.getId() <= 0) throw new IllegalArgumentException("ID inválido");
        validateEmployee(employee, true);
        LOG.info("Actualizando empleado ID {}: {}", employee.getId(), employee.getName());
        return dao.updateEmployee(employee);
    }

    @Override
    public boolean delete(int id) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido");
        LOG.info("Eliminando empleado ID {}", id);
        return dao.deleteEmployee(id);
    }

    @Override
    public List<Employee> top10EmployeesBySalary() {
        // 1. Configuración del mapper para snake_case y fechas ISO
        mapper
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new JavaTimeModule());

        String[] files = {
                "employees_data1.json",
                "employees_data2.json",
                "employees_data3.json"
        };

        // 2. Lectura de cada JSON en paralelo
        List<CompletableFuture<List<Employee>>> futures = Arrays.stream(files)
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try (InputStream is =
                                 Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream("json/" + file)) {
                        if (is == null) {
                            throw new IllegalStateException("Recurso no encontrado: " + file);
                        }
                        return mapper.readValue(is, new TypeReference<List<Employee>>() {});
                    } catch (Exception e) {
                        LOG.error("Error leyendo JSON {}", file, e);
                        throw new RuntimeException("Error al procesar salarios", e);
                    }
                }))
                .toList();

        // 3. Combinar resultados y esperar a que terminen
        List<Employee> all = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        // 4. Ordenar por salario descendente y devolver los primeros 10
        return all.stream()
                .sorted(Comparator.comparing(Employee::getSalary).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }




    /**
     * Valida los datos obligatorios de un empleado.
     *
     * @param e          empleado a validar
     * @param requireId  si debe validar que ID > 0 (para update)
     */
    private void validateEmployee(Employee e, boolean requireId) {
        if (requireId && e.getId() <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }
        if (e.getName() == null || e.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (e.getPosition() == null || e.getPosition().isBlank()) {
            throw new IllegalArgumentException("La posición es obligatoria");
        }
        if (e.getDepartment() == null || e.getDepartment().isBlank()) {
            throw new IllegalArgumentException("El departamento es obligatorio");
        }
        if (e.getHire_date() == null) {
            throw new IllegalArgumentException("La fecha de contratación es obligatoria");
        }
        if (e.getSalary() == null || e.getSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El salario debe ser ≥ 0");
        }
    }
}
