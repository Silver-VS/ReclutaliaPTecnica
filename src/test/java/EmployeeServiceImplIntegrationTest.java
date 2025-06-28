import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import model.Employee;
import model.EmployeeDAOImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import service.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para {@link EmployeeServiceImpl} junto con EmployeeDAOImpl
 * usando Testcontainers y los JSON de recursos para top10Salaries.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmployeeServiceImplIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("employees_test")
            .withUsername("test")
            .withPassword("test");

    private static EmployeeServiceImpl service;

    @BeforeAll
    public static void setup() throws Exception {
        // Configurar Hikari
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(mysql.getJdbcUrl() + "?allowMultiQueries=true");
        cfg.setUsername(mysql.getUsername());
        cfg.setPassword(mysql.getPassword());
        DataSource ds = new HikariDataSource(cfg);

        // Ejecutar DDL y SPs
        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement()) {
            String ddl = Files.readString(Path.of("sql/01_create_employees_table.sql"), StandardCharsets.UTF_8);
            st.execute(ddl);
            for (String script : List.of(
                    "sql/01_sp_create_employee.sql",
                    "sql/02_sp_get_employee_by_id.sql",
                    "sql/03_sp_list_employees.sql",
                    "sql/04_sp_update_employee.sql",
                    "sql/05_sp_delete_employee.sql"
            )) {
                String sql = Files.readString(Path.of(script), StandardCharsets.UTF_8)
                        .replaceAll("(?m)^DELIMITER .*?\\R", "")
                        .replace("END //", "END;");
                st.execute(sql);
            }
        }

        // Instanciar DAO y Service
        EmployeeDAOImpl dao = new EmployeeDAOImpl(ds);
        service = new EmployeeServiceImpl(dao);
    }

    @Test
    @Order(1)
    public void testCreateAndGetById() {
        Employee emp = new Employee();
        emp.setName("John Doe");
        emp.setPosition("Analyst");
        emp.setSalary(new BigDecimal("5000.00"));
        emp.setHire_date(LocalDate.of(2025, 6, 27));
        emp.setDepartment("Finance");

        int id = service.create(emp);
        assertTrue(id > 0, "El ID debe ser mayor que 0");

        Optional<Employee> found = service.getEmployeeById(id);
        assertTrue(found.isPresent(), "Debe encontrar el empleado creado");
        Employee e = found.get();
        assertEquals("John Doe", e.getName());
        assertEquals("Analyst", e.getPosition());
        assertEquals("Finance", e.getDepartment());
        assertEquals(LocalDate.of(2025, 6, 27), e.getHire_date());
    }

    @Test
    @Order(2)
    public void testListEmployees() {
        List<Employee> list = service.findAll();
        assertFalse(list.isEmpty(), "La lista de empleados no debe estar vacía");
    }

    @Test
    @Order(3)
    public void testUpdateEmployee() {
        Employee e = service.findAll().getFirst();
        e.setSalary(new BigDecimal("6000.00"));
        e.setPosition("Senior Analyst");
        boolean updated = service.update(e);
        assertTrue(updated, "La actualización debe retornar true");

        Optional<Employee> reloaded = service.getEmployeeById(e.getId());
        assertTrue(reloaded.isPresent());
        assertEquals(new BigDecimal("6000.00"), reloaded.get().getSalary());
        assertEquals("Senior Analyst", reloaded.get().getPosition());
    }

    @Test
    @Order(4)
    public void testDeleteEmployee() {
        Employee e = service.findAll().getFirst();
        boolean deleted = service.delete(e.getId());
        assertTrue(deleted, "Debe retornar true cuando se elimina");
        assertFalse(service.getEmployeeById(e.getId()).isPresent(), "No debe encontrar el empleado eliminado");
    }

    @Test
    @Order(5)
    public void testTop10EmployeesBySalary() {
        // Verifica que se obtengan 10 empleados desde los JSON
        List<Employee> top = service.top10EmployeesBySalary();
        assertNotNull(top, "La lista de top empleados no debe ser nula");
        assertEquals(10, top.size(), "Debe devolver exactamente 10 empleados");

        // Asegura que estén ordenados por salario descendente
        for (int i = 1; i < top.size(); i++) {
            BigDecimal prev = top.get(i - 1).getSalary();
            BigDecimal curr = top.get(i).getSalary();
            assertTrue(prev.compareTo(curr) >= 0,
                    String.format("Salario %s debe ser >= %s", prev, curr));
        }
    }
}
