package com.DigitalBank.backend.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.DigitalBank.backend.cucumber.steps.common.TestContext;

/**
 * Configuración de beans para tests de Cucumber.
 * Separada del paquete glue para evitar duplicación de beans.
 */
@Configuration
public class CucumberTestConfig {

    @Bean
    public TestContext testContext() {
        return new TestContext();
    }
}
