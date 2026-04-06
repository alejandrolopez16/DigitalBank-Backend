package com.DigitalBank.backend.config;

import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

//@Component
public class GraphQlPublicOperationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_OPERATIONS = Set.of("login", "registrarCliente");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!"/graphql".equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String authHeader = wrappedRequest.getHeader("Authorization");
        boolean hasJwt = authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7;

        if (!hasJwt && !isPublicOperationOnly(wrappedRequest.getCachedBodyAsString())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Debe autenticarse con JWT para ejecutar esta operación\"}");
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean isPublicOperationOnly(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return false;
        }

        try {
            String query = extractQuery(requestBody);
            if (query == null || query.isBlank()) {
                return false;
            }

            Document document = Parser.parse(query);
            for (Definition<?> definition : document.getDefinitions()) {
                if (definition instanceof OperationDefinition operationDefinition) {
                    if (operationDefinition.getSelectionSet() == null) {
                        return false;
                    }
                    for (var selection : operationDefinition.getSelectionSet().getSelections()) {
                        if (!(selection instanceof Field field)) {
                            return false;
                        }
                        if (!PUBLIC_OPERATIONS.contains(field.getName())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractQuery(String requestBody) {
        int queryKeyIndex = requestBody.indexOf("\"query\"");
        if (queryKeyIndex < 0) {
            return null;
        }

        int colonIndex = requestBody.indexOf(':', queryKeyIndex);
        if (colonIndex < 0) {
            return null;
        }

        int firstQuoteIndex = requestBody.indexOf('"', colonIndex + 1);
        if (firstQuoteIndex < 0) {
            return null;
        }

        StringBuilder escapedQuery = new StringBuilder();
        boolean escaped = false;
        for (int i = firstQuoteIndex + 1; i < requestBody.length(); i++) {
            char current = requestBody.charAt(i);
            if (escaped) {
                escapedQuery.append(current);
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                escapedQuery.append(current);
                continue;
            }
            if (current == '"') {
                break;
            }
            escapedQuery.append(current);
        }

        return escapedQuery.toString()
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        String getCachedBodyAsString() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
