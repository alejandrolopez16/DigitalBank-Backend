package com.DigitalBank.backend.cucumber.steps.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase compartida para mantener el estado entre diferentes steps de Cucumber.
 * Permite compartir datos entre Given, When y Then steps del mismo escenario.
 */
public class TestContext {
    private final Map<String, Object> context = new HashMap<>();

    public void set(String key, Object value) {
        context.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) context.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) context.get(key);
    }

    public boolean contains(String key) {
        return context.containsKey(key);
    }

    public void clear() {
        context.clear();
    }
}
