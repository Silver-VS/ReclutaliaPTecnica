/*
 * EmployeeApiE2ETest.java
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

package e2e;

import api.EmployeeApiServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import model.Employee;
import model.EmployeeDAO;
import model.EmployeeDAOImpl;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import service.EmployeeService;
import service.EmployeeServiceImpl;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de extremo a extremo para la API de empleados.
 *
 * <p>
 * - Usa Testcontainers para MySQL.<br>
 * - Aplica scripts DDL y stored procedures al inicio.<br>
 * - Arranca el {@link EmployeeApiServer} en un puerto dinámico.<br>
 * - Usa {@link HttpClient} para ejecutar flujos CRUD HTTP.
 * </p>
 *
 * @author Silver
 * @version 1.0
 * @since 2025-06-27
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmployeeApiE2ETest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("employees_test_e2e")
            .withUsername("test")
            .withPassword("test");

    private HttpServer httpServer;
    private String baseUrl;
    private HttpClient client;

    @BeforeAll
    void setup() throws Exception {
        // 1) Configurar HikariCP para el contenedor
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(mysql.getJdbcUrl());
        cfg.setUsername(mysql.getUsername());
        cfg.setPassword(mysql.getPassword());
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        cfg.setInitializationFailTimeout(-1); // no fail-fast
        cfg.setMinimumIdle(0);               // sin conexiones iniciales
        cfg.setMaximumPoolSize(5);
        HikariDataSource ds = new HikariDataSource(cfg);

        // 2) Aplicar DDL y SP desde /sql
        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement()) {

            // DDL
            String ddl = Files.readString(Paths.get("sql", "01_create_employees_table.sql"),
                    StandardCharsets.UTF_8);
            st.execute(ddl);

            // Stored procedures
            List<String> scripts = List.of(
                    "01_sp_create_employee.sql",
                    "02_sp_get_employee_by_id.sql",
                    "03_sp_list_employees.sql",
                    "04_sp_update_employee.sql",
                    "05_sp_delete_employee.sql"
            );
            for (String s : scripts) {
                String raw = Files.readString(Paths.get("sql", s),
                        StandardCharsets.UTF_8);
                String cleaned = raw
                        .replaceAll("(?m)^DELIMITER .*?$", "")
                        .replace("END //", "END;");
                st.execute(cleaned);
            }
        }

        // 3) Arrancar la API en puerto aleatorio (0)
        EmployeeDAO dao = new EmployeeDAOImpl(ds);
        EmployeeService svc = new EmployeeServiceImpl(dao);
        EmployeeApiServer server = new EmployeeApiServer(svc);
        httpServer = server.startServer(0);
        int port = httpServer.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
        client  = HttpClient.newHttpClient();
    }

    @AfterAll
    void tearDown() {
        // Detener el servidor
        httpServer.stop(0);
    }

    @Test
    void testCreateGetListDeleteFlow() throws Exception {
        // --- CREATE ---
        String payload = JSON.writeValueAsString(
                Map.of(
                        "firstName", "Mario",
                        "lastName",  "Rossi",
                        "email",     "mario.rossi@example.com",
                        "salary",    5500.00
                )
        );
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/employees"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> createRes = client.send(createReq, BodyHandlers.ofString());
        assertEquals(201, createRes.statusCode(), "Create debe devolver 201");
        int newId = JSON.readTree(createRes.body()).get("id").asInt();
        assertTrue(newId > 0, "El ID debe ser positivo");

        // --- GET by ID ---
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/employees/" + newId))
                .GET()
                .build();
        HttpResponse<String> getRes = client.send(getReq, BodyHandlers.ofString());
        assertEquals(200, getRes.statusCode(), "GetById debe devolver 200");
        Employee emp = JSON.readValue(getRes.body(), Employee.class);
        assertEquals("Mario", emp.getFirstName());
        assertEquals("Rossi", emp.getLastName());

        // --- LIST ---
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/employees"))
                .GET()
                .build();
        HttpResponse<String> listRes = client.send(listReq, BodyHandlers.ofString());
        assertEquals(200, listRes.statusCode(), "List debe devolver 200");
        List<?> arr = JSON.readValue(listRes.body(), List.class);
        assertFalse(arr.isEmpty(), "La lista no debe estar vacía");

        // --- DELETE ---
        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/employees/" + newId))
                .DELETE()
                .build();
        HttpResponse<Void> delRes = client.send(delReq, BodyHandlers.discarding());
        assertEquals(204, delRes.statusCode(), "Delete debe devolver 204");

        // --- GET tras delete = 404 ---
        HttpResponse<String> get404 = client.send(getReq, BodyHandlers.ofString());
        assertEquals(404, get404.statusCode(), "GetById tras delete debe devolver 404");
    }
}

