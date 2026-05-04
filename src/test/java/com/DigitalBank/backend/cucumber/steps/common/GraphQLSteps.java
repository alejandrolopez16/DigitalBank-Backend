package com.DigitalBank.backend.cucumber.steps.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GraphQLSteps {

    @Autowired
    private TestContext testContext;

    @Autowired
    private Environment environment;

    private final RestTemplate restTemplate = new RestTemplate();

    private String authToken;
    private Map<String, String> authHeaders = new HashMap<>();
    private ErrorResponse lastErrorResponse;

    private String getBaseUrl() {
        String port = environment.getProperty("local.server.port");
        if (port == null) {
            port = "8080";
        }
        return "http://localhost:" + port + "/graphql";
    }

    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> executeQuery(String query, Class<T> responseType) {
        try {
            String jsonBody = String.format("{\"query\":\"%s\"}", escapeGraphQL(query));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!authHeaders.isEmpty() && authHeaders.containsKey("Authorization")) {
                headers.set("Authorization", authHeaders.get("Authorization"));
            }

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<Map> rawResponse = restTemplate.postForEntity(
                getBaseUrl(),
                request,
                Map.class
            );

            if (rawResponse.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = rawResponse.getBody();
                if (body != null && body.containsKey("errors")) {
                    List<Map<String, Object>> errors = (List<Map<String, Object>>) body.get("errors");
                    StringBuilder errorMsg = new StringBuilder();
                    for (Map<String, Object> err : errors) {
                        errorMsg.append(err.get("message")).append("; ");
                    }
                    if (responseType == ErrorResponse.class) {
                        ErrorResponse errorResp = new ErrorResponse();
                        errorResp.message = errorMsg.toString();
                        errorResp.errors = errors;
                        lastErrorResponse = errorResp;
                        return (ResponseEntity<T>) ResponseEntity.status(400).body(errorResp);
                    }
                    return (ResponseEntity<T>) ResponseEntity.status(400).body(body);
                }
                if (responseType == Map.class) {
                    return (ResponseEntity<T>) ResponseEntity.ok(body);
                }
                return (ResponseEntity<T>) ResponseEntity.ok(body);
            } else {
                if (responseType == ErrorResponse.class) {
                    ErrorResponse errorResp = new ErrorResponse();
                    errorResp.message = "HTTP " + rawResponse.getStatusCode().value();
                    lastErrorResponse = errorResp;
                    return (ResponseEntity<T>) ResponseEntity.status(rawResponse.getStatusCode()).body(errorResp);
                }
                return ResponseEntity.status(rawResponse.getStatusCode()).build();
            }
        } catch (RestClientException e) {
            if (responseType == ErrorResponse.class) {
                ErrorResponse errorResp = new ErrorResponse();
                errorResp.message = "Connection error: " + e.getMessage();
                lastErrorResponse = errorResp;
                return (ResponseEntity<T>) ResponseEntity.status(500).body(errorResp);
            }
            return ResponseEntity.status(500).build();
        }
    }

    private String escapeGraphQL(String query) {
        return query.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    public String buildMutation(String mutationName, String parameters) {
        return String.format("mutation { %s(%s) }", mutationName, parameters);
    }

    public String buildQuery(String queryName, String parameters) {
        return String.format("query { %s(%s) }", queryName, parameters);
    }

    public static class ErrorResponse {
        private String message;
        private List<Map<String, Object>> errors;

        public String getMessage() {
            return message;
        }

        public List<Map<String, Object>> getErrors() {
            return errors;
        }
    }

    public String buildAuthenticationToken(String email, String password) {
        String query = String.format(
            "mutation { login(email: \"%s\", passwordHash: \"%s\") { token } }",
            email, password
        );
        try {
            String jsonBody = String.format("{\"query\":\"%s\"}", escapeGraphQL(query));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl(),
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                // QA NOTE: Verificar si hay errores en la respuesta
                if (body.containsKey("errors")) {
                    System.out.println("QA WARNING - Login falló para " + email + ": " + body.get("errors"));
                    // Retornar null para indicar que no se pudo autenticar
                    return null;
                }
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null) {
                    Map<String, Object> loginData = (Map<String, Object>) data.get("login");
                    if (loginData != null && loginData.get("token") != null) {
                        authToken = loginData.get("token").toString();
                        testContext.set("authToken", authToken);
                        return authToken;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("QA ERROR - Excepción durante login: " + e.getMessage());
            return null;
        }
        return null;
    }

    public void setAuthenticationHeader(String token) {
        if (token != null) {
            authHeaders.put("Authorization", "Bearer " + token);
            testContext.set("authToken", token);
        } else {
            System.out.println("QA WARNING - Se intentó establecer autenticación con token null");
        }
    }

    public void clearAuthentication() {
        authToken = null;
        authHeaders.clear();
        testContext.clear();
    }

    public TestContext getTestContext() {
        return testContext;
    }

    public ErrorResponse getLastErrorResponse() {
        return lastErrorResponse;
    }
}
