import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import model.Employee;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import service.EmployeeService;
import service.EmployeeServiceImpl;
import model.EmployeeDAOImpl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import api.*;

import javax.sql.DataSource;

/**
 * Integration tests for EmployeeLambdaHandler using a real MySQL container.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmployeeLambdaHandlerIntegrationTest {
    private Thread serverThread;
    private static MySQLContainer<?> mysql;

    @BeforeAll
    void startServer() throws Exception {
        // Start a MySQL container for E2E testing
        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        mysql.start();

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

            st.executeUpdate(
                    "INSERT INTO employees(name, position, salary, hire_date, department) " +
                            "VALUES('Init','Tester',1234.00,'2025-06-27','FakeDepartment')"
            );
        }

        // Override DB env vars to use the container
        System.setProperty("USE_AWS_SECRETS", "false");
        System.setProperty("DB_URL", mysql.getJdbcUrl());
        System.setProperty("DB_USER", mysql.getUsername());
        System.setProperty("DB_PASS", mysql.getPassword());

        // Start local API server with real DAO and service
        serverThread = new Thread(() -> {
            try {
                new EmployeeApiServer(
                        new EmployeeServiceImpl(
                                new EmployeeDAOImpl()
                        )
                ).startLocal(8080);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Give the server time to start
        Thread.sleep(500);

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    @AfterAll
    void stopServer() {
        if (serverThread != null) {
            serverThread.interrupt();
        }
        if (mysql != null) {
            mysql.stop();
        }
    }

    @Test
    @Order(1)
    void testGetTop10EmployeesBySalary() {
        List<Employee> top = given()
                .when().get("/employees/salary/top")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(10))
                .extract().jsonPath().getList("", Employee.class);

        // Verify descending salary order
        for (int i = 1; i < top.size(); i++) {
            BigDecimal prev = top.get(i - 1).getSalary();
            BigDecimal curr = top.get(i).getSalary();
            Assertions.assertTrue(
                    prev.compareTo(curr) >= 0,
                    String.format("Salary %s should be >= %s", prev, curr)
            );
        }
    }

    @Test
    @Order(2)
    void testGetEmployeeById() {
        given()
                .when().get("/employees/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1));
    }

    @Test
    @Order(3)
    void testCreateEmployee() {
        Employee newEmp = new Employee();
        newEmp.setName("John Doe");
        newEmp.setPosition("Developer");
        newEmp.setSalary(BigDecimal.valueOf(5000));
        newEmp.setHire_date(LocalDate.of(2025, 1, 1));
        newEmp.setDepartment("IT");

        given()
                .contentType(ContentType.JSON)
                .body(newEmp)
                .when().post("/employees")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", greaterThan(0));
    }

    @Test
    @Order(4)
    void testUpdateEmployee() {
        Employee upd = new Employee();
        upd.setName("Jane Smith");
        upd.setPosition("Lead");
        upd.setSalary(BigDecimal.valueOf(6000));
        upd.setHire_date(LocalDate.of(2025, 2, 1));
        upd.setDepartment("Engineering");

        given()
                .contentType(ContentType.JSON)
                .body(upd)
                .when().put("/employees/1")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(5)
    void testDeleteEmployee() {
        given()
                .when().delete("/employees/1")
                .then()
                .statusCode(204);
    }
}
