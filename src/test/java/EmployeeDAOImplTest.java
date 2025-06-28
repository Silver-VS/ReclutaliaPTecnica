import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import model.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas de integración para {@link EmployeeDAOImpl} usando Testcontainers.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmployeeDAOImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeDAOImplTest.class);

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("employees_test")
            .withUsername("test")
            .withPassword("test");

    private static EmployeeDAO dao;

    @BeforeAll
    public static void setup() throws Exception {
        // Configuramos Hikari con allowMultiQueries para ejecutar varios bloques
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(mysql.getJdbcUrl() + "?allowMultiQueries=true");
        cfg.setUsername(mysql.getUsername());
        cfg.setPassword(mysql.getPassword());
        DataSource ds = new HikariDataSource(cfg);

        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement()) {

            // DDL
            String ddl = Files.readString(Path.of("sql/01_create_employees_table.sql"), StandardCharsets.UTF_8);
            st.execute(ddl);

            // Stored procedures
            for (String script : List.of(
                    "sql/01_sp_create_employee.sql",
                    "sql/02_sp_get_employee_by_id.sql",
                    "sql/03_sp_list_employees.sql",
                    "sql/04_sp_update_employee.sql",
                    "sql/05_sp_delete_employee.sql"
            )) {
                String raw = Files.readString(Path.of(script), StandardCharsets.UTF_8)
                        .replaceAll("(?m)^DELIMITER .*?$", "")
                        .replace("END //", "END;");
                st.execute(raw);
            }
        }

        dao = new EmployeeDAOImpl(ds);
        LOG.info("DAO listo para pruebas");
    }

    @Test
    @Order(1)
    public void testCreateAndGetById() {
        LOG.info("Test 1: create & getById");
        Employee emp = new Employee(
                "Jane Smith",                 // name
                "Developer",                  // position
                new BigDecimal("7500.00"),     // salary
                LocalDate.of(2025, 6, 26),    // hireDate
                "Engineering"                // department
        );

        int id = dao.createEmployee(emp);
        assertTrue(id > 0, "El ID debe ser mayor a 0");

        Optional<Employee> found = dao.getEmployeeById(id);
        assertTrue(found.isPresent(), "Debe existir el empleado");
        assertEquals("Jane Smith", found.get().getName());
        assertEquals("Developer",  found.get().getPosition());
        assertEquals("Engineering", found.get().getDepartment());
        assertEquals(LocalDate.of(2025, 6, 26), found.get().getHire_date());
    }

    @Test
    @Order(2)
    public void testListEmployees() {
        LOG.info("Test 2: listEmployees");
        List<Employee> list = dao.getAllEmployees();
        assertFalse(list.isEmpty(), "La lista no debe estar vacía");
    }

    @Test
    @Order(3)
    public void testUpdateEmployee() {
        LOG.info("Test 3: updateEmployee");
        Employee e = dao.getAllEmployees().getFirst();
        e.setSalary(new BigDecimal("8000.00"));
        e.setPosition("Senior Developer");
        assertTrue(dao.updateEmployee(e), "Debe actualizarse correctamente");

        Optional<Employee> updated = dao.getEmployeeById(e.getId());
        assertTrue(updated.isPresent());
        assertEquals(new BigDecimal("8000.00"), updated.get().getSalary());
        assertEquals("Senior Developer", updated.get().getPosition());
    }

    @Test
    @Order(4)
    public void testDeleteEmployee() {
        LOG.info("Test 4: deleteEmployee");
        Employee e = dao.getAllEmployees().getFirst();
        assertTrue(dao.deleteEmployee(e.getId()), "Debe eliminarse correctamente");
        assertFalse(dao.getEmployeeById(e.getId()).isPresent(), "No debe existir más el empleado");
    }
}
