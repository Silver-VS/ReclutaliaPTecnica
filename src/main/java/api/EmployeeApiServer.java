package api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.EmployeeDAOImpl;
import model.Employee;
import service.EmployeeService;
import service.EmployeeServiceImpl;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * AWS Lambda handler y arranque local para la API REST de empleados.
 */
public class EmployeeApiServer implements RequestStreamHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final EmployeeService service;

    // Constructor por defecto (producción / Lambda)
    public EmployeeApiServer() {
        this(new EmployeeServiceImpl(new EmployeeDAOImpl()));
    }

    // Constructor de inyección (por tests)
    public EmployeeApiServer(EmployeeService service) {
        this.service = service;
    }



    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        APIGatewayProxyResponseEvent response;
        try {
            APIGatewayProxyRequestEvent event = MAPPER.readValue(input, APIGatewayProxyRequestEvent.class);
            response = route(event);
        } catch (Exception e) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
        output.write(MAPPER.writeValueAsBytes(response));
    }

    private APIGatewayProxyResponseEvent route(APIGatewayProxyRequestEvent event) throws IOException {
        String method = event.getHttpMethod();
        String path   = event.getPath();
        String body   = event.getBody();
        Map<String,String> headers = Map.of("Content-Type", "application/json");

        // POST /employees
        if ("POST".equals(method) && "/employees".equals(path)) {
            Employee in = MAPPER.readValue(body, Employee.class);
            int newId = service.create(in);
            in.setId(newId);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withHeaders(headers)
                    .withBody(MAPPER.writeValueAsString(in));
        }

        // GET /employees
        if ("GET".equals(method) && "/employees".equals(path)) {
            List<Employee> all = service.findAll();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(MAPPER.writeValueAsString(all));
        }

        // GET /employees/salary/top
        if ("GET".equals(method) && "/employees/salary/top".equals(path)) {
            List<?> top = service.top10EmployeesBySalary();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(MAPPER.writeValueAsString(top));
        }

        // Rutas con ID
        if (path != null && path.startsWith("/employees/")) {
            String[] parts = path.split("/");
            if (parts.length == 3) {
                try {
                    int id = Integer.parseInt(parts[2]);
                    if ("GET".equals(method)) {
                        Optional<Employee> f = service.getEmployeeById(id);
                        return f.map(emp -> {
                                    try {
                                        return new APIGatewayProxyResponseEvent()
                                                        .withStatusCode(200)
                                                        .withHeaders(headers)
                                                        .withBody(MAPPER.writeValueAsString(emp));
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .orElseGet(() -> new APIGatewayProxyResponseEvent()
                                        .withStatusCode(404)
                                        .withHeaders(headers)
                                        .withBody("{\"error\":\"Not Found\"}"));
                    }
                    if ("PUT".equals(method)) {
                        Employee in = MAPPER.readValue(body, Employee.class);
                        in.setId(id);
                        boolean ok = service.update(in);
                        if (ok) {
                            // 204 No Content, sin body
                            return new APIGatewayProxyResponseEvent()
                                    .withStatusCode(204)
                                    .withHeaders(headers);
                        } else {
                            // 404 si no existe
                            return new APIGatewayProxyResponseEvent()
                                    .withStatusCode(404)
                                    .withHeaders(headers)
                                    .withBody("{\"error\":\"Employee not found\"}");
                        }
                    }

                    if ("DELETE".equals(method)) {
                        boolean ok = service.delete(id);
                        return new APIGatewayProxyResponseEvent()
                                .withStatusCode(ok ? 204 : 404)
                                .withHeaders(headers);
                    }
                } catch (NumberFormatException ex) {
                    return new APIGatewayProxyResponseEvent()
                            .withStatusCode(400)
                            .withHeaders(headers)
                            .withBody("{\"error\":\"Invalid ID\"}");
                }
            }
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(headers)
                .withBody("{\"error\":\"Not Found\"}");
    }

    // ====================================
    // Arranque local embebido con HttpServer
    // ====================================

    public void startLocal(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        HttpHandler handler = this::handleExchange;
        server.createContext("/employees", handler);
        server.createContext("/employees/salary/top", handler);
        server.createContext("/employees/", handler);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Servidor arrancado en http://localhost:" + port);
    }

    private void handleExchange(HttpExchange exch) throws IOException {
        try {
            // Construir un mini-request con path, method y body
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            exch.getRequestBody().transferTo(baos);
            APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                    .withHttpMethod(exch.getRequestMethod())
                    .withPath(exch.getRequestURI().getPath())
                    .withBody(baos.toString(StandardCharsets.UTF_8));

            APIGatewayProxyResponseEvent resp = route(event);

            exch.getResponseHeaders().add("Content-Type", "application/json");
            String jsonBody = resp.getBody() != null ? resp.getBody() : "";
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            exch.sendResponseHeaders(resp.getStatusCode(), bytes.length);
            try (OutputStream os = exch.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String err = "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}";
            byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
            exch.getResponseHeaders().add("Content-Type", "application/json");
            exch.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exch.getResponseBody()) {
                os.write(bytes);
            }
        } finally {
            exch.close();
        }
    }

    public static void main(String[] args) throws IOException {
        new EmployeeApiServer().startLocal(8080);
    }
}
