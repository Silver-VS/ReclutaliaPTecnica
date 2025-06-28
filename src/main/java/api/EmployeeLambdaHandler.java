/*
 * EmployeeLambdaHandler.java
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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.Employee;
import model.EmployeeDAOImpl;
import service.EmployeeService;
import service.EmployeeServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lambda handler para la API de empleados. Maneja rutas mínimas:
 * - GET /employees/salary/top
 * - GET /employees/{id}
 * - POST /employees
 * - PUT /employees/{id}
 * - DELETE /employees/{id}
 */
public class EmployeeLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final EmployeeService service =
            new EmployeeServiceImpl(new EmployeeDAOImpl());

    private final ObjectMapper mapper;

    public EmployeeLambdaHandler() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent req,
            Context ctx) {
        String path = req.getPath();
        String httpMethod = req.getHttpMethod();

        try {
            if ("GET".equals(httpMethod) && "/employees/salary/top".equals(path)) {
                List<Employee> top = service.top10EmployeesBySalary();
                return respond(200, top);
            }

            int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));

            if ("GET".equals(httpMethod) && path.matches("/employees/\\d+")) {
                return service.getEmployeeById(id)
                        .map(emp -> {
                            try {
                                return respond(200, emp);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .orElse(notFound());
            }

            if ("POST".equals(httpMethod) && "/employees".equals(path)) {
                Employee e = mapper.readValue(req.getBody(), Employee.class);
                int newId = service.create(e);
                Map<String,Object> body = new HashMap<>();
                body.put("id", newId);
                return respond(201, body);
            }

            if ("PUT".equals(httpMethod) && path.matches("/employees/\\d+")) {
                Employee e = mapper.readValue(req.getBody(), Employee.class);
                e.setId(id);
                boolean updated = service.update(e);
                return updated ? noContent() : notFound();
            }

            if ("DELETE".equals(httpMethod) && path.matches("/employees/\\d+")) {
                boolean deleted = service.delete(id);
                return deleted ? noContent() : notFound();
            }

            return notFound();

        } catch (IllegalArgumentException | JsonProcessingException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            ctx.getLogger().log("Internal error: " + ex.getMessage());
            return serverError();
        }
    }

    private APIGatewayProxyResponseEvent respond(int statusCode, Object body) throws JsonProcessingException {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(mapper.writeValueAsString(body));
    }

    private APIGatewayProxyResponseEvent notFound() {
        return new APIGatewayProxyResponseEvent().withStatusCode(404);
    }

    private APIGatewayProxyResponseEvent badRequest(String message) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(Map.of("Content-Type", "text/plain"))
                .withBody(message);
    }

    private APIGatewayProxyResponseEvent serverError() {
        return new APIGatewayProxyResponseEvent().withStatusCode(500);
    }

    private APIGatewayProxyResponseEvent noContent() {
        return new APIGatewayProxyResponseEvent().withStatusCode(204);
    }
}

