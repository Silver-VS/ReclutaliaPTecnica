/*
 * EmployeeApiServer.java
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
package api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.Employee;
import model.EmployeeDAO;
import model.EmployeeDAOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.EmployeeService;
import service.EmployeeServiceImpl;

import javax.sql.DataSource;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Servidor HTTP embebido que expone una API REST para gestión de empleados.
 *
 * <p>
 * - Realiza primero una verificación puntual de conexión con
 * {@code DatabaseConfig.verifySingleConnection()}.
 * - Configura y arranca un pool lazily con HikariCP.
 * - Registra peticiones y errores (stack trace) con SLF4J.
 * </p>
 *
 * @author Silver
 * @version 1.3
 * @since 2025-06-27
 */
public class EmployeeApiServer {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeApiServer.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final EmployeeService service;

    /**
     * Construye el servidor con el servicio de negocio inyectado.
     *
     * @param service implementación de {@link EmployeeService}
     */
    public EmployeeApiServer(EmployeeService service) {
        this.service = service;
    }

    /**
     * Punto de entrada.
     *
     * @param args no usados
     * @throws Exception si no puede iniciarse
     */
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080").trim());
        LOG.info("Arrancando servidor en puerto {}", port);

        // Prueba puntual sin pool
        try {
            config.DatabaseConfig.verifySingleConnection();
        } catch (Exception ex) {
            LOG.error("✖ Prueba puntual fallida: {}", ex.getMessage(), ex);
        }

        DataSource ds = config.DatabaseConfig.getDataSource();
        EmployeeDAO dao = new EmployeeDAOImpl(ds);
        EmployeeService svc = new EmployeeServiceImpl(dao);

        new EmployeeApiServer(svc).start(port);
    }

    /**
     * Inicia el servidor HTTP en el puerto dado y registra contextos.
     *
     * @param port puerto para HTTP
     * @throws Exception si falla al crear o arrancar
     */
    public void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        LOG.info("Servidor HTTP iniciado en el puerto {}", port);

        server.createContext("/employees", new EmployeesHandler());
        server.createContext("/employees/salary/top", new TopSalaryHandler());

        server.setExecutor(null);
        server.start();
        LOG.info("Contextos registrados: /employees, /employees/salary/top");
    }

    private void logRequest(HttpExchange exchange) {
        LOG.info("Petición entrante: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
    }

    private int extractId(HttpExchange exchange) {
        String p = exchange.getRequestURI().getPath();
        String idStr = p.substring(p.lastIndexOf('/') + 1);
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID inválido: " + idStr, e);
        }
    }

    private void sendJson(HttpExchange exchange, int code, Object payload) {
        try {
            byte[] b = JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(code, b.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(b);
            }
            LOG.debug("Respondido {} con {} bytes", code, b.length);
        } catch (Exception e) {
            LOG.error("Error enviando JSON:", e);
        }
    }

    private class EmployeesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            logRequest(exchange);
            try {
                switch (exchange.getRequestMethod()) {
                    case "GET" -> sendJson(exchange, 200, service.findAll());
                    case "POST" -> {
                        Employee e = JSON.readValue(exchange.getRequestBody(), Employee.class);
                        int id = service.create(e);
                        sendJson(exchange, 201, Map.of("id", id));
                    }
                    case "PUT" -> {
                        Employee e = JSON.readValue(exchange.getRequestBody(), Employee.class);
                        e.setId(extractId(exchange));
                        service.update(e);
                        sendJson(exchange, 204, "");
                    }
                    case "DELETE" -> {
                        service.delete(extractId(exchange));
                        sendJson(exchange, 204, "");
                    }
                    default -> sendJson(exchange, 405, Map.of("error", "Método no permitido"));
                }
            } catch (IllegalArgumentException iae) {
                LOG.warn("Validación fallida: {}", iae.getMessage(), iae);
                sendJson(exchange, 400, Map.of("error", iae.getMessage()));
            } catch (Exception e) {
                LOG.error("Error procesando petición:", e);
                sendJson(exchange, 500, Map.of("error", "Error interno del servidor"));
            } finally {
                exchange.close();
            }
        }
    }

    private class TopSalaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            logRequest(exchange);
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, Map.of("error", "Método no permitido"));
                    return;
                }
                sendJson(exchange, 200, service.top10Salaries());
            } catch (Exception e) {
                LOG.error("Error procesando top10Salaries:", e);
                sendJson(exchange, 500, Map.of("error", "Error interno del servidor"));
            } finally {
                exchange.close();
            }
        }
    }
}
