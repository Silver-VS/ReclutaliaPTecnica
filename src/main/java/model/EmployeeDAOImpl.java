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
 * Implementación de {@link EmployeeDAO} que utiliza JDBC y stored procedures
 * para persistir y recuperar datos de empleados.
 *
 * @author Silver VS
 * @version 1.2
 * @since 2025-06-27
 */
public class EmployeeDAOImpl implements EmployeeDAO {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeDAOImpl.class);
    private final DataSource dataSource;

    public EmployeeDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public EmployeeDAOImpl() {
        this(DatabaseConfig.getDataSource());
    }

    @Override
    public int createEmployee(Employee employee) {
        final String sql = "{CALL sp_create_employee(?, ?, ?, ?, ?, ?)}";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, employee.getName());
            cs.setString(2, employee.getPosition());
            cs.setBigDecimal(3, employee.getSalary());
            cs.setDate(4, Date.valueOf(employee.getHire_date()));
            cs.setString(5, employee.getDepartment());
            cs.registerOutParameter(6, Types.INTEGER);

            cs.execute();
            int newId = cs.getInt(6);
            LOG.info("Empleado creado con ID {}", newId);
            return newId;

        } catch (SQLException e) {
            LOG.error("Error en sp_create_employee", e);
            throw new RuntimeException("Error al crear empleado", e);
        }
    }

    @Override
    public Optional<Employee> getEmployeeById(int id) {
        final String sql = "{CALL sp_get_employee_by_id(?)}";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToEmployee(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            LOG.error("Error en sp_get_employee_by_id", e);
            throw new RuntimeException("Error al obtener empleado", e);
        }
    }

    @Override
    public List<Employee> getAllEmployees() {
        final String sql = "{CALL sp_list_employees()}";
        List<Employee> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToEmployee(rs));
            }
            return list;

        } catch (SQLException e) {
            LOG.error("Error en sp_list_employees", e);
            throw new RuntimeException("Error al listar empleados", e);
        }
    }

    @Override
    public boolean updateEmployee(Employee employee) {
        final String sql = "{CALL sp_update_employee(?, ?, ?, ?, ?, ?)}";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, employee.getId());
            cs.setString(2, employee.getName());
            cs.setString(3, employee.getPosition());
            cs.setBigDecimal(4, employee.getSalary());
            cs.setDate(5, Date.valueOf(employee.getHire_date()));
            cs.setString(6, employee.getDepartment());

            int rows = cs.executeUpdate();
            LOG.info("sp_update_employee afectó {} fila(s)", rows);
            return rows > 0;

        } catch (SQLException e) {
            LOG.error("Error en sp_update_employee", e);
            throw new RuntimeException("Error al actualizar empleado", e);
        }
    }

    @Override
    public boolean deleteEmployee(int id) {
        final String sql = "{CALL sp_delete_employee(?)}";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, id);
            int rows = cs.executeUpdate();
            LOG.info("sp_delete_employee afectó {} fila(s)", rows);
            return rows > 0;

        } catch (SQLException e) {
            LOG.error("Error en sp_delete_employee", e);
            throw new RuntimeException("Error al eliminar empleado", e);
        }
    }

    /**
     * Mapea una fila de ResultSet a un objeto Employee.
     */
    private Employee mapRowToEmployee(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getInt("id"));
        e.setName(rs.getString("name"));
        e.setSalary(rs.getBigDecimal("salary"));
        e.setDepartment(rs.getString("department"));
        Date d = rs.getDate("hire_date");
        e.setPosition(rs.getString("position"));
        e.setHire_date(d != null ? d.toLocalDate() : null);
        return e;
    }
}
