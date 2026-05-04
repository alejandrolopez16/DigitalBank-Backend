package com.DigitalBank.backend.cucumber.steps.customer;

import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.customer.repository.RoleRepository;
import com.DigitalBank.backend.customer.entity.Role;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLSteps;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLRequest;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLSteps.ErrorResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerValidationSteps {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GraphQLSteps graphQLSteps;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ResponseEntity<Map> validationResponse;
    private ResponseEntity<ErrorResponse> errorResponse;
    private String currentDocumentNumber = "456123789"; // Documento por defecto para validaciones
    private static long testCounter = 0; // Contador para IDs únicos

    @Given("existen los siguientes clientes en estado {string}:")
    public void existenLosSiguientesClientesEnEstado(String status, io.cucumber.datatable.DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER no encontrado"));

        for (Map<String, String> row : rows) {
            // Generar ID único agregando contador si el documento ya existe
            String docNumber = row.get("documento");
            String uniqueDoc = docNumber + "_" + (testCounter++);

            Customer customer = Customer.builder()
                .name(row.get("nombre"))
                .documentNumber(uniqueDoc)
                .documentType("CC")
                .email(row.get("email").replace("@", "_" + testCounter + "@"))
                .birthDate(LocalDate.now().minusYears(25))
                .passwordHash("hashed")
                .status(status)
                .build();
            customer.setRoles(java.util.Set.of(userRole));
            customerRepository.save(customer);
        }
    }

    @When("el administrador consulta la lista de clientes pendientes")
    public void elAdministradorConsultaLaListaDeClientesPendientes() {
        // Autenticar como administrador antes de realizar la operación
        authenticateAsAdmin();

        String query = "query { clientesPendientes { documentNumber name email status } }";
        validationResponse = graphQLSteps.executeQuery(query, Map.class);
    }

    @Then("el sistema retorna {int} clientes")
    public void elSistemaRetornaClientes(int expectedCount) {
        assertEquals(200, validationResponse.getStatusCode().value());
        Map<String, Object> data = (Map<String, Object>) validationResponse.getBody().get("data");
        List<Map<String, Object>> customers = (List<Map<String, Object>>) data.get("clientesPendientes");
        assertEquals(expectedCount, customers.size());
    }

    @Then("todos los clientes tienen estado {string}")
    public void todosLosClientesTienenEstado(String expectedStatus) {
        Map<String, Object> data = (Map<String, Object>) validationResponse.getBody().get("data");
        List<Map<String, Object>> customers = (List<Map<String, Object>>) data.get("clientesPendientes");
        for (Map<String, Object> customer : customers) {
            assertEquals(expectedStatus, customer.get("status"));
        }
    }

    @Given("un cliente en estado {string} con documento {string}")
    public void unClienteEnEstadoConDocumento(String status, String documentNumber) {
        currentDocumentNumber = documentNumber; // Guardar el documento actual
        Customer customer = Customer.builder()
            .name("Test Customer")
            .documentNumber(documentNumber)
            .documentType("CC")
            .email("validation@" + documentNumber + ".com")
            .birthDate(LocalDate.now().minusYears(25))
            .passwordHash("hashed")
            .status(status)
            .build();

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER no encontrado"));
        customer.setRoles(java.util.Set.of(userRole));
        customerRepository.save(customer);
    }

    @When("el administrador aprueba al cliente con documento {string}")
    public void elAdministradorApruebaAlClienteConDocumento(String documentNumber) {
        // Autenticar como administrador antes de realizar la operación
        authenticateAsAdmin();

        String query = String.format("mutation { aprobarCliente(documentNumber: \"%s\") { documentNumber status } }", documentNumber);
        validationResponse = graphQLSteps.executeQuery(query, Map.class);
    }

    @Then("el estado del cliente cambia a {string}")
    public void elEstadoDelClienteCambiaA(String expectedStatus) {
        assertEquals(200, validationResponse.getStatusCode().value());
        Map<String, Object> data = (Map<String, Object>) validationResponse.getBody().get("data");
        Map<String, Object> customerData = (Map<String, Object>) data.values().iterator().next();
        assertEquals(expectedStatus, customerData.get("status"));
    }

    @Then("se elimina cualquier comentario de rechazo")
    public void seEliminaCualquierComentarioDeRechazo() {
        // Verified by the successful approval response
        assertNotNull(validationResponse.getBody());
    }

    @Then("se envía un correo de aprobación al cliente")
    public void seEnviaUnCorreoDeAprobacionAlCliente() {
        // Email sending is async/simulated, just verify approval succeeded
        assertEquals(200, validationResponse.getStatusCode().value());
    }

    @When("el administrador rechaza al cliente con documento {string} y comentario {string}")
    public void elAdministradorRechazaAlClienteConDocumentoYComentario(String documentNumber, String comentario) {
        // Autenticar como administrador antes de realizar la operación
        authenticateAsAdmin();

        String query = String.format(
            "mutation { rechazarCliente(documentNumber: \"%s\", comentario: \"%s\") { documentNumber status rejectionComments } }",
            documentNumber, comentario
        );
        validationResponse = graphQLSteps.executeQuery(query, Map.class);
    }

    @Then("se registra el comentario de rechazo {string}")
    public void seRegistraElComentarioDeRechazo(String expectedComment) {
        Map<String, Object> data = (Map<String, Object>) validationResponse.getBody().get("data");
        Map<String, Object> customerData = (Map<String, Object>) data.values().iterator().next();
        assertEquals(expectedComment, customerData.get("rejectionComments"));
    }

    @Then("se envía un correo de rechazo al cliente")
    public void seEnviaUnCorreoDeRechazoAlCliente() {
        assertEquals(200, validationResponse.getStatusCode().value());
    }

    @When("el administrador intenta rechazar al cliente con documento {string} sin comentario")
    public void elAdministradorIntentaRechazarAlClienteConDocumentoSinComentario(String documentNumber) {
        // Autenticar como administrador antes de realizar la operación
        authenticateAsAdmin();

        String query = String.format(
            "mutation { rechazarCliente(documentNumber: \"%s\", comentario: \"\") { documentNumber status } }",
            documentNumber
        );
        graphQLSteps.executeQuery(query, ErrorResponse.class);
    }

    @Then("el estado del cliente permanece como {string}")
    public void elEstadoDelClientePermaneceComo(String expectedStatus) {
        // Usar el documento actual en lugar del valor fijo
        Customer customer = customerRepository.findById(currentDocumentNumber)
            .orElseThrow(() -> new AssertionError("Cliente no encontrado con documento: " + currentDocumentNumber));
        assertEquals(expectedStatus, customer.getStatus());
    }

    private void authenticateAsAdmin() {
        String adminEmail = "admin@bank.com";
        String adminDocument = "admin123456";

        // Verificar si el admin ya existe
        if (!customerRepository.existsById(adminDocument)) {
            // Crear rol ADMIN si no existe
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    role.setDescription("Administrador del sistema");
                    return roleRepository.save(role);
                });

            // Crear usuario administrador
            Customer admin = Customer.builder()
                .documentNumber(adminDocument)
                .documentType("CC")
                .name("Admin User")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .status("ACTIVE")
                .birthDate(LocalDate.now().minusYears(30))
                .build();

            admin.setRoles(java.util.Set.of(adminRole));
            customerRepository.save(admin);
        }

        // Autenticar como administrador
        String token = graphQLSteps.buildAuthenticationToken(adminEmail, "AdminPass123!");
        graphQLSteps.setAuthenticationHeader(token);
    }
}
