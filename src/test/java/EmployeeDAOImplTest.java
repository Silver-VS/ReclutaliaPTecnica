/*
 * EmployeeDAOImplTest.java
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

import model.Employee;
import model.EmployeeDAO;
import model.EmployeeDAOImpl;

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
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas de integración para {@link EmployeeDAOImpl} usando Testcontainers.
 * <p>
 * Verifica que la capa DAO ejecute correctamente los stored procedures
 * contra una instancia de MySQL aislada.
 * Las configuraciones (imagen, BD, credenciales) provienen de variables de entorno
 * para mantener consistencia con los entornos local y CI.
 * </p>
 *
 * @author Silver VS
 * @version 1.5
 * @since 2025-06-27
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmployeeDAOImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeDAOImplTest.class);

    /**
     * Contenedor MySQL para pruebas.
     * <p>
     * Variables de entorno:
     * - TEST_MYSQL_IMAGE: imagen Docker (ej. mysql:8.0.33)
     * - TEST_DB_NAME: nombre de la base de datos
     * - DB_USER, DB_PASS: credenciales de acceso
     * </p>
     */
    @Container
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> mysql = new MySQLContainer<>(
            System.getenv().getOrDefault("TEST_MYSQL_IMAGE", "mysql:8.0.33")
    )
            .withDatabaseName(System.getenv().getOrDefault("TEST_DB_NAME", "employees_test"))
            .withUsername(System.getenv().getOrDefault("DB_USER", "test"))
            .withPassword(System.getenv().getOrDefault("DB_PASS", "test"));

    private static EmployeeDAO dao;

    /**
     * Inicializa el entorno de pruebas:
     * - Levanta el contenedor MySQL.
     * - Configura HikariCP.
     * - Aplica DDL y SPs desde los archivos SQL.
     * - Crea la instancia DAO.
     *
     * @throws Exception si hay error al leer archivos o ejecutar SQL
     */
    @BeforeAll
    public static void setup() throws Exception {
        LOG.info("Arrancando contenedor MySQL: {}", mysql.getDockerImageName());
        DataSource ds = new HikariDataSource(createHikariConfig(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
        LOG.info("Conectado a URL de prueba: {}", mysql.getJdbcUrl());

        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            LOG.debug("Aplicando DDL...");
            String ddl = loadFileAsString("01_create_employees_table.sql");
            st.execute(ddl);

            LOG.debug("Aplicando stored procedures...");
            for (String script : List.of(
                    "01_sp_create_employee.sql",
                    "02_sp_get_employee_by_id.sql",
                    "03_sp_list_employees.sql",
                    "04_sp_update_employee.sql",
                    "05_sp_delete_employee.sql"
            )) {
                String rawSql = loadFileAsString(script);
                // Elimina líneas DELIMITER y ajusta el END
                String cleaned = rawSql
                        .replaceAll("(?m)^DELIMITER .*?$", "")
                        .replace("END //", "END;");
                st.execute(cleaned);
            }
        }

        dao = new EmployeeDAOImpl(ds);
        LOG.info("DAO listo para pruebas");
    }

    /**
     * Verifica la creación y lectura de un empleado.
     */
    @Test
    @Order(1)
    public void testCreateAndGetById() {
        LOG.info("Test: create & getById");
        Employee emp = new Employee("Jane", "Smith", "jane.smith@example.com", new BigDecimal("7500.00"));
        int id = dao.createEmployee(emp);
        assertTrue(id > 0, "El ID debe ser mayor a 0");

        Optional<Employee> found = dao.getEmployeeById(id);
        assertTrue(found.isPresent(), "Debe existir el empleado");
        assertEquals("Jane", found.get().getFirstName(), "El nombre debe coincidir");
    }

    /**
     * Verifica que la lista de empleados incluya al menos un registro.
     */
    @Test
    @Order(2)
    public void testListEmployees() {
        LOG.info("Test: listEmployees");
        assertFalse(dao.getAllEmployees().isEmpty(), "La lista no debe estar vacía");
    }

    /**
     * Verifica la actualización de un empleado.
     */
    @Test
    @Order(3)
    public void testUpdateEmployee() {
        LOG.info("Test: updateEmployee");
        Employee e = dao.getAllEmployees().getFirst();
        e.setSalary(new BigDecimal("8000.00"));
        assertTrue(dao.updateEmployee(e), "Debe actualizarse correctamente");

        Optional<Employee> updated = dao.getEmployeeById(e.getId());
        assertTrue(updated.isPresent());
        assertEquals(new BigDecimal("8000.00"), updated.get().getSalary());
    }

    /**
     * Verifica la eliminación de un empleado.
     */
    @Test
    @Order(4)
    public void testDeleteEmployee() {
        LOG.info("Test: deleteEmployee");
        Employee e = dao.getAllEmployees().getFirst();
        assertTrue(dao.deleteEmployee(e.getId()), "Debe eliminarse correctamente");
        assertFalse(dao.getEmployeeById(e.getId()).isPresent(), "No debe existir más el empleado");
    }

    /**
     * Lee un archivo SQL de la carpeta raiz 'sql'.
     *
     * @param fileName nombre de archivo dentro de 'sql'
     * @return contenido del archivo
     * @throws Exception si no existe o falla lectura
     */
    private static String loadFileAsString(String fileName) throws Exception {
        Path path = Paths.get("sql", fileName);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * Configura HikariCP con un pool reducido para pruebas.
     *
     * @param url  JDBC URL
     * @param user usuario
     * @param pass contraseña
     * @return configuración de HikariConfig
     */
    private static HikariConfig createHikariConfig(String url, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(2);
        cfg.setMinimumIdle(1);
        return cfg;
    }
}
