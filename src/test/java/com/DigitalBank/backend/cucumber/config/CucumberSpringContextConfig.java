package com.DigitalBank.backend.cucumber.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.DigitalBank.backend.DigitalbankApplication;
import com.DigitalBank.backend.test.CucumberTestConfig;

/**
 * Configuración de Spring Context para los tests de Cucumber.
 * Levanta el contexto completo de Spring Boot para los acceptance tests.
 */
@CucumberContextConfiguration
@Import(CucumberTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = DigitalbankApplication.class)
public class CucumberSpringContextConfig {
}
