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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.EmployeeService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Servidor HTTP embebido que expone una API REST para la gestión de empleados.
 * <p>
 * Esta clase proporciona métodos para arrancar y detener el servidor,
 * así como handlers internos para los endpoints:
 * <ul>
 *   <li><code>/employees</code> CRUD completo y listados</li>
 *   <li><code>/employees/{id}</code> operaciones sobre un único empleado</li>
 *   <li><code>/employees/salary/top</code> top 10 de salarios</li>
 * </ul>
 * El servidor utiliza SLF4J para registro y Jackson para serialización JSON.
 * </p>
 *
 * <p>
 * Para pruebas de extremo a extremo, dispone de:
 * <ul>
 *   <li>{@link #startServer(int)} que arranca en un puerto dado (0 = aleatorio).</li>
 *   <li>{@link #stop(int)} que detiene el servidor con un retraso configurable.</li>
 * </ul>
 * </p>
 *
 * @author Silver
 * @version 1.4
 * @since 2025-06-27
 */
public class EmployeeApiServer {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeApiServer.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final EmployeeService service;
    private HttpServer server;

    /**
     * Construye un <code>EmployeeApiServer</code> con la capa de servicio
     * proporcionada para la lógica de negocio.
     *
     * @param service implementación de {@link EmployeeService} para todas las operaciones
     */
    public EmployeeApiServer(EmployeeService service) {
        this.service = service;
    }

    /**
     * Crea y arranca el servidor HTTP en el puerto especificado.
     * <p>
     * Registra los contexts:
     * <ul>
     *   <li><code>/employees</code> junto con lógica CRUD</li>
     *   <li><code>/employees/salary/top</code> para top 10 salarios</li>
     * </ul>
     * </p>
     *
     * @param port puerto TCP en el que escuchar (0 para elegir uno aleatorio)
     * @return la instancia interna de {@link HttpServer} arrancada
     * @throws IOException si ocurre un error al crear o arrancar el servidor
     */
    public HttpServer startServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        LOG.info("Servidor HTTP iniciándose en puerto {}", port);

        server.createContext("/employees", new EmployeesHandler());
        server.createContext("/employees/salary/top", new TopSalaryHandler());
        server.setExecutor(null);
        server.start();

        int actualPort = server.getAddress().getPort();
        LOG.info("Servidor HTTP iniciado en puerto {}", actualPort);
        LOG.info("Contextos registrados: /employees, /employees/salary/top");
        return server;
    }

    /**
     * Detiene el servidor HTTP.
     *
     * @param delay segundos a esperar antes de forzar el shutdown
     */
    public void stop(int delay) {
        if (server != null) {
            server.stop(delay);
            LOG.info("Servidor HTTP detenido (delay={}s)", delay);
        }
    }

    /**
     * Registra una petición entrante en los logs.
     *
     * @param exchange intercambio HTTP actual
     */
    private void logRequest(HttpExchange exchange) {
        LOG.info("Petición entrante: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());
    }

    /**
     * Extrae y parsea el ID de recurso de la URI.
     *
     * @param exchange intercambio HTTP actual
     * @return el ID extraído como entero
     * @throws IllegalArgumentException si el segmento final no es un entero válido
     */
    private int extractId(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID inválido: " + idStr, e);
        }
    }

    /**
     * Envía una respuesta JSON al cliente o, si el código es 204 (No Content),
     * solo envía la cabecera sin cuerpo.
     *
     * @param exchange intercambio HTTP actual
     * @param code     código de estado HTTP a devolver
     * @param payload  objeto a serializar como JSON (se ignora si code == 204)
     */
    private void sendJson(HttpExchange exchange, int code, Object payload) {
        try {
            if (code == 204) {
                // No Content: enviar solo cabecera
                exchange.getResponseHeaders()
                        .set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(204, -1);
                LOG.debug("Respondido {} (sin contenido)", code);
                return;
            }

            byte[] bytes = JSON.writeValueAsString(payload)
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders()
                    .set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            LOG.debug("Respondido {} con {} bytes", code, bytes.length);

        } catch (Exception e) {
            LOG.error("Error enviando respuesta JSON:", e);
        }
    }

    /**
     * Handler para todas las rutas bajo <code>/employees</code>,
     * soportando GET (list y getById), POST, PUT y DELETE.
     */
    private class EmployeesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            logRequest(exchange);
            String method = exchange.getRequestMethod();
            String path   = exchange.getRequestURI().getPath();

            try {
                if ("GET".equals(method)) {
                    // LIST: GET /employees
                    if ("/employees".equals(path) || "/employees/".equals(path)) {
                        sendJson(exchange, 200, service.findAll());
                        return;
                    }
                    // GET /employees/{id}
                    if (path.startsWith("/employees/")) {
                        int id = extractId(exchange);
                        Optional<Employee> emp = service.findById(id);
                        if (emp.isPresent()) {
                            sendJson(exchange, 200, emp.get());
                        } else {
                            sendJson(exchange, 404, Map.of("error", "Empleado no encontrado"));
                        }
                        return;
                    }
                    sendJson(exchange, 404, Map.of("error", "Recurso no encontrado"));
                    return;
                }

                if ("POST".equals(method) && ("/employees".equals(path) || "/employees/".equals(path))) {
                    // CREATE: POST /employees
                    Employee toCreate = JSON.readValue(exchange.getRequestBody(), Employee.class);
                    int newId = service.create(toCreate);
                    sendJson(exchange, 201, Map.of("id", newId));
                    return;
                }

                if ("PUT".equals(method) && path.startsWith("/employees/")) {
                    // UPDATE: PUT /employees/{id}
                    Employee toUpdate = JSON.readValue(exchange.getRequestBody(), Employee.class);
                    toUpdate.setId(extractId(exchange));
                    service.update(toUpdate);
                    sendJson(exchange, 204, "");
                    return;
                }

                if ("DELETE".equals(method) && path.startsWith("/employees/")) {
                    // DELETE: DELETE /employees/{id}
                    service.delete(extractId(exchange));
                    sendJson(exchange, 204, "");
                    return;
                }

                // Método o ruta no soportada
                sendJson(exchange, 405, Map.of("error", "Método o ruta no permitida"));

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

    /**
     * Handler para <code>/employees/salary/top</code>, devolviendo
     * el top 10 de empleados por salario.
     */
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
