package com.DigitalBank.backend.cucumber.steps.customer;

import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.DigitalBank.backend.customer.entity.AuthResponse;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.customer.repository.RoleRepository;
import com.DigitalBank.backend.customer.entity.Role;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLSteps;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLRequest;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLSteps.ErrorResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions para el flujo de autenticación de clientes (HU-1.1.3).
 */
public class CustomerAuthenticationSteps {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GraphQLSteps graphQLSteps;

    private ResponseEntity<Map> loginResponse;
    private ResponseEntity<ErrorResponse> errorResponse;

    // QA NOTE: Campo adicional para manejar errores de otras operaciones (transferencias, etc.)
    private ResponseEntity<GraphQLSteps.ErrorResponse> operationErrorResponse;

    @Given("un cliente activo con email {string} y contraseña {string}")
    public void unClienteActivoConEmailYContrasena(String email, String password) {
        Customer customer = Customer.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .status("ACTIVE")
            .documentNumber("123456789")
            .documentType("CC")
            .name("Test User")
            .birthDate(LocalDate.now().minusYears(25))
            .loginAttempts(0)
            .build();

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER no encontrado"));

        customer.setRoles(java.util.Set.of(userRole));
        customerRepository.save(customer);
    }

    @When("el usuario inicia sesión con email {string} y contraseña {string}")
    public void elUsuarioIniciaSesionConEmailYContrasena(String email, String password) {
        String query = buildLoginMutation(email, password);
        GraphQLRequest request = new GraphQLRequest(query);
        loginResponse = graphQLSteps.executeQuery(query, Map.class);
    }

    @Then("el sistema genera un token JWT válido")
    public void elSistemaGeneraUnTokenJWTValido() {
        assertEquals(200, loginResponse.getStatusCode().value());

        Map<String, Object> data = (Map<String, Object>) loginResponse.getBody().get("data");
        Map<String, Object> loginData = (Map<String, Object>) data.get("login");

        String token = (String) loginData.get("token");
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Validar formato JWT (3 partes separadas por .)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Token JWT debe tener 3 partes");
    }

    @And("el sistema retorna el estado {string}")
    public void elSistemaRetornaElEstado(String expectedStatus) {
        Map<String, Object> data = (Map<String, Object>) loginResponse.getBody().get("data");
        Map<String, Object> loginData = (Map<String, Object>) data.get("login");

        assertEquals(expectedStatus, loginData.get("status"));
    }

    @And("el sistema retorna el rol del usuario")
    public void elSistemaRetornaElRolDelUsuario() {
        Map<String, Object> data = (Map<String, Object>) loginResponse.getBody().get("data");
        Map<String, Object> loginData = (Map<String, Object>) data.get("login");

        String role = (String) loginData.get("role");
        assertNotNull(role);
        assertFalse(role.isEmpty());
    }

    @And("se resetean los intentos fallidos a {int}")
    public void seReseteanLosIntentosFallidos(int expectedAttempts) {
        Customer customer = customerRepository.findByEmail("user@bank.com")
            .orElseThrow(() -> new AssertionError("Cliente no encontrado"));

        assertEquals(expectedAttempts, customer.getLoginAttempts());
    }

    @And("se elimina el tiempo de bloqueo si existía")
    public void seEliminaElTiempoDeBloqueoSiExistia() {
        Customer customer = customerRepository.findByEmail("user@bank.com")
            .orElseThrow(() -> new AssertionError("Cliente no encontrado"));

        assertNull(customer.getLockTime());
    }

    @When("el usuario intenta iniciar sesión con contraseña incorrecta {int} veces")
    public void elUsuarioIntentaIniciarSesionConContrasenaIncorrectaVeces(int attempts) {
        Customer customer = customerRepository.findByEmail("user@bank.com")
            .orElseThrow(() -> new AssertionError("Cliente no encontrado"));

        for (int i = 0; i < attempts; i++) {
            String query = buildLoginMutation("user@bank.com", "wrongpassword");
            GraphQLRequest request = new GraphQLRequest(query);
            errorResponse = graphQLSteps.executeQuery(query, ErrorResponse.class);
        }
    }

    @Then("la cuenta se bloquea por 10 minutos")
    public void laCuentaSeBloqueaPor10Minutos() {
        Customer customer = customerRepository.findByEmail("user@bank.com")
            .orElseThrow(() -> new AssertionError("Cliente no encontrado"));

        assertNotNull(customer.getLockTime());
        assertEquals(3, customer.getLoginAttempts());
    }

    @And("el contador de intentos fallidos es {int}")
    public void elContadorDeIntentosFallidosEs(int expectedAttempts) {
        Customer customer = customerRepository.findByEmail("user@bank.com")
            .orElseThrow(() -> new AssertionError("Cliente no encontrado"));

        assertEquals(expectedAttempts, customer.getLoginAttempts());
    }

    @And("se registra el tiempo de bloqueo")
    public void seRegistraElTiempoDeBloqueo() {
        Customer customer = customerRepository.findByEmail("user@bank.com")
            .orElseThrow(() -> new AssertionError("Cliente no encontrado"));

        assertNotNull(customer.getLockTime());
        assertTrue(customer.getLockTime().isBefore(LocalDateTime.now()));
    }

    @Given("un cliente con cuenta bloqueada hasta 10 minutos después de los intentos fallidos")
    public void unClienteConCuentaBloqueadaHasta10MinutosDespuesDeLosIntentosFallidos() {
        Customer customer = Customer.builder()
            .email("user@bank.com")
            .passwordHash(passwordEncoder.encode("SecurePass123!"))
            .status("ACTIVE")
            .documentNumber("123456789")
            .documentType("CC")
            .name("Test User")
            .birthDate(LocalDate.now().minusYears(25))
            .loginAttempts(3)
            .lockTime(LocalDateTime.now())
            .build();

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER no encontrado"));

        customer.setRoles(java.util.Set.of(userRole));
        customerRepository.save(customer);
    }

    @When("el usuario intenta iniciar sesión con contraseña correcta")
    public void elUsuarioIntentaIniciarSesionConContrasenaCorrecta() {
        String query = buildLoginMutation("user@bank.com", "SecurePass123!");
        loginResponse = graphQLSteps.executeQuery(query, Map.class);
    }

    @Then("el sistema rechaza el inicio de sesión")
    public void elSistemaRechazaElInicioDeSesion() {
        // QA NOTE: Verificar que loginResponse no sea null
        assertNotNull(loginResponse, "loginResponse no debería ser null");
        Map<String, Object> body = loginResponse.getBody();
        assertTrue(body.containsKey("errors"), "Expected login to fail with errors");
    }

    @And("el mensaje indica {string}")
    public void elMensajeIndica(String expectedMessage) {
        // QA NOTE: Manejar tanto errores de autenticación como de transferencia
        // Verificar si es un error de autenticación (loginResponse) o de transferencia (errorResponse)
        String actualMessage = null;

        if (loginResponse != null) {
            // Error de autenticación
            Map<String, Object> body = loginResponse.getBody();
            if (body != null && body.containsKey("errors")) {
                List<Map<String, Object>> errors = (List<Map<String, Object>>) body.get("errors");
                if (errors != null && !errors.isEmpty()) {
                    actualMessage = errors.get(0).get("message").toString();
                }
            }
        } else if (errorResponse != null && errorResponse.getBody() != null) {
            // Error de transferencia u otras operaciones
            actualMessage = errorResponse.getBody().getMessage();
        }

        assertNotNull(actualMessage, "No se pudo obtener el mensaje de error");
        assertTrue(actualMessage.contains(expectedMessage),
            "Mensaje esperado: '" + expectedMessage + "', Mensaje real: '" + actualMessage + "'");
    }

    @Given("un cliente con cuenta bloqueada hace 15 minutos")
    public void unClienteConCuentaBloqueadaHace15Minutos() {
        Customer customer = Customer.builder()
            .email("user@bank.com")
            .passwordHash(passwordEncoder.encode("SecurePass123!"))
            .status("ACTIVE")
            .documentNumber("123456789")
            .documentType("CC")
            .name("Test User")
            .birthDate(LocalDate.now().minusYears(25))
            .loginAttempts(3)
            .lockTime(LocalDateTime.now().minusMinutes(15))
            .build();

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER no encontrado"));

        customer.setRoles(java.util.Set.of(userRole));
        customerRepository.save(customer);
    }

    @And("el sistema permite el inicio de sesión")
    public void elSistemaPermiteElInicioDeSesion() {
        // QA NOTE: Verificar que loginResponse no sea null
        assertNotNull(loginResponse, "loginResponse no debería ser null");
        Map<String, Object> body = loginResponse.getBody();
        assertNotNull(body, "El cuerpo de la respuesta no debería ser null");
        assertFalse(body.containsKey("errors"), "Expected login to succeed without errors");
    }

    @And("se elimina el tiempo de bloqueo")
    public void seEliminaElTiempoDeBloqueo() {
        Customer customer = customerRepository.findByEmail("user@bank.com")
            .orElseThrow(() -> new AssertionError("Cliente no encontrado"));
        assertNull(customer.getLockTime());
    }

    @And("se genera un token JWT válido")
    public void seGeneraUnTokenJWTValido() {
        // QA NOTE: Verificar que loginResponse no sea null
        assertNotNull(loginResponse, "loginResponse no debería ser null al verificar token");
        Map<String, Object> body = loginResponse.getBody();
        assertNotNull(body, "El cuerpo de la respuesta no debería ser null");
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertNotNull(data, "Los datos de la respuesta no deberían ser null");
        Map<String, Object> loginResult = (Map<String, Object>) data.get("login");
        assertNotNull(loginResult, "El resultado del login no debería ser null");
        String token = (String) loginResult.get("token");
        assertNotNull(token, "El token no debería ser null");
        assertFalse(token.isEmpty(), "El token no debería estar vacío");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Token JWT debe tener 3 partes");
    }

    @And("el cuarto intento de login falla con bloqueo")
    public void elCuartoIntentoDeLoginFallaConBloqueo() {
        Customer customer = customerRepository.findByEmail("user@bank.com")
            .orElseThrow(() -> new AssertionError("Cliente no encontrado"));
        assertNotNull(customer.getLockTime());
        assertEquals(3, customer.getLoginAttempts());
    }

    private String buildLoginMutation(String email, String password) {
        return String.format(
            "mutation { login(email: \"%s\", passwordHash: \"%s\") { " +
            "token message status role } }",
            email, password
        );
    }
}
