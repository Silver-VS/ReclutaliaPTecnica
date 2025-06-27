/*
 * EmployeeDAOImpl.java
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

import config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link EmployeeDAO} que utiliza JDBC y stored procedures.
 * <p>
 * Se apoya en {@link DatabaseConfig} para obtener el pool de conexiones.
 * Registra eventos clave mediante SLF4J.
 *
 * @author Silver VS
 * @version 1.1
 * @since 2025-06-26
 */
public class EmployeeDAOImpl implements EmployeeDAO {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeDAOImpl.class);
    private final DataSource dataSource;

    /**
     * Constructor que permite inyección de un DataSource.
     *
     * @param dataSource instancia de {@link DataSource} a usar
     */
    public EmployeeDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Constructor por defecto: utiliza {@link DatabaseConfig#getDataSource()}.
     */
    public EmployeeDAOImpl() {
        this(DatabaseConfig.getDataSource());
    }

    @Override
    public int createEmployee(Employee employee) {
        String sql = "{CALL sp_create_employee(?, ?, ?, ?, ?)}";
        try (Connection conn = dataSource.getConnection(); CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, employee.getFirstName());
            stmt.setString(2, employee.getLastName());
            stmt.setString(3, employee.getEmail());
            stmt.setBigDecimal(4, employee.getSalary());
            stmt.registerOutParameter(5, Types.INTEGER);

            stmt.execute();
            int newId = stmt.getInt(5);
            LOG.info("Empleado creado con ID {}", newId);
            return newId;

        } catch (SQLException e) {
            LOG.error("Error en sp_create_employee", e);
            throw new RuntimeException("Error al crear empleado", e);
        }
    }

    @Override
    public Optional<Employee> getEmployeeById(int id) {
        String sql = "{CALL sp_get_employee_by_id(?)}";
        try (Connection conn = dataSource.getConnection(); CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Employee emp = mapRowToEmployee(rs);
                    return Optional.of(emp);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error en sp_get_employee_by_id", e);
            throw new RuntimeException("Error al obtener empleado", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Employee> getAllEmployees() {
        String sql = "{CALL sp_list_employees()}";
        List<Employee> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); CallableStatement stmt = conn.prepareCall(sql); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToEmployee(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error en sp_list_employees", e);
            throw new RuntimeException("Error al listar empleados", e);
        }
        return list;
    }

    @Override
    public boolean updateEmployee(Employee employee) {
        String sql = "{CALL sp_update_employee(?, ?, ?, ?, ?, ?)}";
        try (Connection conn = dataSource.getConnection(); CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, employee.getId());
            stmt.setString(2, employee.getFirstName());
            stmt.setString(3, employee.getLastName());
            stmt.setString(4, employee.getEmail());
            stmt.setBigDecimal(5, employee.getSalary());
            stmt.registerOutParameter(6, Types.INTEGER);

            stmt.execute();
            int rows = stmt.getInt(6);
            LOG.info("sp_update_employee afectó {} fila(s)", rows);
            return rows > 0;

        } catch (SQLException e) {
            LOG.error("Error en sp_update_employee", e);
            throw new RuntimeException("Error al actualizar empleado", e);
        }
    }

    @Override
    public boolean deleteEmployee(int id) {
        String sql = "{CALL sp_delete_employee(?, ?)}";
        try (Connection conn = dataSource.getConnection(); CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, id);
            stmt.registerOutParameter(2, Types.INTEGER);

            stmt.execute();
            int rows = stmt.getInt(2);
            LOG.info("sp_delete_employee afectó {} fila(s)", rows);
            return rows > 0;

        } catch (SQLException e) {
            LOG.error("Error en sp_delete_employee", e);
            throw new RuntimeException("Error al eliminar empleado", e);
        }
    }

    /**
     * Mapea una fila de ResultSet a un objeto Employee.
     *
     * @param rs ResultSet posicionado en la fila actual
     * @return instancia de {@link Employee} con datos de la fila
     * @throws SQLException en caso de fallo al leer columnas
     */
    private Employee mapRowToEmployee(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getInt("id"));
        e.setFirstName(rs.getString("first_name"));
        e.setLastName(rs.getString("last_name"));
        e.setEmail(rs.getString("email"));
        e.setSalary(rs.getBigDecimal("salary"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        e.setUpdatedAt(rs.getTimestamp("updated_at"));
        return e;
    }
}