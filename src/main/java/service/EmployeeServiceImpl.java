/*
 * EmployeeServiceImpl.java
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Employee;
import model.EmployeeDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Implementación de {@link EmployeeService} que aplica reglas de negocio
 * y orquesta operaciones CRUD mediante {@link EmployeeDAO}.
 * <p>
 * Valida datos antes de delegar al DAO y combina múltiples fuentes JSON
 * para calcular el top 10 de salarios.
 * </p>
 *
 * @author Silver VS
 * @version 1.0
 * @since 2025-06-27
 */
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeServiceImpl.class);
    private final EmployeeDAO dao;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor que recibe la implementación del DAO.
     *
     * @param dao implementación de acceso a datos
     */
    public EmployeeServiceImpl(EmployeeDAO dao) {
        this.dao = dao;
    }

    @Override
    public int create(Employee employee) {
        validateEmployee(employee);
        LOG.info("Creando empleado: {} {}", employee.getFirstName(), employee.getLastName());
        return dao.createEmployee(employee);
    }

    @Override
    public Optional<Employee> findById(int id) {
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
        validateEmployee(employee);
        return dao.updateEmployee(employee);
    }

    @Override
    public boolean delete(int id) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido");
        return dao.deleteEmployee(id);
    }

    @Override
    public List<BigDecimal> top10Salaries() {
        List<BigDecimal> all = new ArrayList<>();
        String[] files = {"employees_data1.json", "employees_data2.json", "employees_data3.json"};
        for (String file : files) {
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("json/" + file)) {
                if (is == null) throw new IllegalStateException("Recurso no encontrado: " + file);
                List<BigDecimal> salaries = mapper.readValue(is, new TypeReference<>() {
                });
                all.addAll(salaries);
            } catch (Exception e) {
                LOG.error("Error leyendo archivo JSON {}", file, e);
                throw new RuntimeException("Error al procesar salarios", e);
            }
        }
        return all.stream()
                .sorted(Collections.reverseOrder())
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Valida que los datos obligatorios de un empleado sean correctos.
     *
     * @param e objeto a validar
     */
    private void validateEmployee(Employee e) {
        if (e.getFirstName() == null || e.getFirstName().isBlank())
            throw new IllegalArgumentException("Nombre es obligatorio");
        if (e.getLastName() == null || e.getLastName().isBlank())
            throw new IllegalArgumentException("Apellido es obligatorio");
        if (e.getEmail() == null || !e.getEmail().contains("@"))
            throw new IllegalArgumentException("Email inválido");
        if (e.getSalary() == null || e.getSalary().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Salario inválido");
    }
}