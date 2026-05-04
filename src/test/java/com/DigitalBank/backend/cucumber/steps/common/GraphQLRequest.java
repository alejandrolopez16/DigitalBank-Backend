package com.DigitalBank.backend.cucumber.steps.common;

/**
 * Request GraphQL para ser enviado al servidor.
 * Clase separada para ser accesible desde las step definitions.
 */
public class GraphQLRequest {
    private final String query;

    public GraphQLRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
