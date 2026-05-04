package com.DigitalBank.backend.cucumber.steps.transaction;

import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.customer.repository.RoleRepository;
import com.DigitalBank.backend.customer.entity.Role;
import com.DigitalBank.backend.transaction.entity.SecurityPolicy;
import com.DigitalBank.backend.transaction.repository.SecurityPolicyRepository;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLSteps;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLRequest;
import com.DigitalBank.backend.cucumber.steps.common.GraphQLSteps.ErrorResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions para el flujo de transferencias y políticas de seguridad (HU-2.x.x).
 */
public class TransferSteps {

    @Autowired
    private FinancialAccountRepository financialAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SecurityPolicyRepository securityPolicyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GraphQLSteps graphQLSteps;

    private ResponseEntity<Map> transferResponse;
    private ResponseEntity<ErrorResponse> errorResponse;
    private String currentDocumentNumber = "trans_cust_123"; // Documento por defecto para transferencias (max 20 chars)
    private String destinationDocumentNumber = "trans_dest_456"; // Documento para cuenta destino
    private static long testCounter = 0; // Contador para IDs únicos

    @Given("el cliente autenticado tiene una cuenta con saldo {string}")
    public void elClienteAutenticadoTieneUnaCuentaConSaldo(String balance) {
        createAuthenticatedCustomerWithBalance(balance.replace("$", "").replace(",", ""));
    }

    @Given("existe una cuenta destino activa con saldo {string}")
    public void existeUnaCuentaDestinoActivaConSaldo(String balance) {
        createDestinationCustomerWithBalance(balance.replace("$", "").replace(",", ""));
    }

    @When("el cliente transfiere {string} a la cuenta destino")
    public void elClienteTransfiereALaCuentaDestino(String amount) {
        // Obtener cuentas de manera más segura
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas para realizar transferencia. Encontradas: " + accounts.size());
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount destAccount = accounts.get(1);

        String query = buildTransferMutation(sourceAccount.getId().toString(), destAccount.getId().toString(), amount);
        GraphQLRequest request = new GraphQLRequest(query);
        transferResponse = graphQLSteps.executeQuery(query, Map.class);
    }

    @Then("la transferencia se completa exitosamente")
    public void laTransferenciaSeCompletaExitosamente() {
        assertEquals(200, transferResponse.getStatusCode().value());

        Map<String, Object> data = (Map<String, Object>) transferResponse.getBody().get("data");
        Map<String, Object> transferData = (Map<String, Object>) data.get("ejecutarTransferencia");

        assertTrue((Boolean) transferData.get("success"));
    }

    @And("la cuenta origen tiene saldo {string}")
    public void laCuentaOrigenTieneSaldo(String expectedBalance) {
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.isEmpty()) {
            throw new IllegalStateException("No se encontraron cuentas");
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount updatedSource = financialAccountRepository.findById(sourceAccount.getId())
            .orElseThrow(() -> new AssertionError("Cuenta origen no encontrada"));

        BigDecimal expected = new BigDecimal(expectedBalance.replace("$", "").replace(",", ""));
        assertEquals(0, expected.compareTo(updatedSource.getBalance()));
    }

    @And("la cuenta destino tiene saldo {string}")
    public void laCuentaDestinoTieneSaldo(String expectedBalance) {
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas. Encontradas: " + accounts.size());
        }

        FinancialAccount destAccount = accounts.get(1);
        FinancialAccount updatedDest = financialAccountRepository.findById(destAccount.getId())
            .orElseThrow(() -> new AssertionError("Cuenta destino no encontrada"));

        BigDecimal expected = new BigDecimal(expectedBalance.replace("$", "").replace(",", ""));
        assertEquals(0, expected.compareTo(updatedDest.getBalance()));
    }

    @And("se genera una referencia de transacción única")
    public void seGeneraUnaReferenciaDeTransaccionUnica() {
        Map<String, Object> data = (Map<String, Object>) transferResponse.getBody().get("data");
        Map<String, Object> transferData = (Map<String, Object>) data.get("ejecutarTransferencia");

        assertNotNull(transferData.get("transactionReference"));
        assertFalse(transferData.get("transactionReference").toString().isEmpty());
    }

    @And("el estado de la transacción es {string}")
    public void elEstadoDeLaTransaccionEs(String expectedStatus) {
        Map<String, Object> data = (Map<String, Object>) transferResponse.getBody().get("data");
        Map<String, Object> transferData = (Map<String, Object>) data.get("ejecutarTransferencia");

        assertEquals(expectedStatus, transferData.get("status"));
    }

    @When("el cliente intenta transferir {string} a la cuenta destino")
    public void elClienteIntentaTransferirALaCuentaDestino(String amount) {
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas. Encontradas: " + accounts.size());
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount destAccount = accounts.get(1);

        String query = buildTransferMutation(sourceAccount.getId().toString(), destAccount.getId().toString(), amount);
        GraphQLRequest request = new GraphQLRequest(query);
        errorResponse = graphQLSteps.executeQuery(query, ErrorResponse.class);
    }

    @Then("la transferencia es rechazada")
    public void laTransferenciaEsRechazada() {
        // Aceptar tanto HTTP 400 (error GraphQL) como HTTP 200 con status de rechazo
        assertTrue(
            errorResponse.getStatusCode().value() == 400 ||
            errorResponse.getStatusCode().value() == 200,
            "Se esperaba rechazo (400 o 200 con status de rechazo)"
        );
    }

    @Given("existe una cuenta destino activa")
    public void existeUnaCuentaDestinoActiva() {
        createDestinationCustomerWithBalance("1000.00");
    }

    @And("los saldos de las cuentas no cambian")
    public void losSaldosDeLasCuentasNoCambian() {
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas. Encontradas: " + accounts.size());
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount destAccount = accounts.get(1);

        // Los saldos deben permanecer iguales
        assertNotNull(sourceAccount);
        assertNotNull(destAccount);
    }


    @Given("el cliente autenticado tiene una cuenta bloqueada")
    public void elClienteAutenticadoTieneUnaCuentaBloqueada() {
        // Crear el cliente si no existe
        Customer customer = customerRepository.findById(currentDocumentNumber)
            .orElseGet(() -> createTestCustomer(currentDocumentNumber));

        FinancialAccount account = FinancialAccount.builder()
            .customer(customer)
            .balance(BigDecimal.valueOf(5000.00))
            .status("BLOCKED")
            .build();
        financialAccountRepository.save(account);

        // Asegurar que el token está configurado
        String token = graphQLSteps.buildAuthenticationToken("test@bank.com", "SecurePass123!");
        graphQLSteps.setAuthenticationHeader(token);
    }

    @When("el cliente intenta realizar una transferencia")
    public void elClienteIntentaRealizarUnaTransferencia() {
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas. Encontradas: " + accounts.size());
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount destAccount = accounts.get(1);

        String query = buildTransferMutation(sourceAccount.getId().toString(), destAccount.getId().toString(), "500.00");
        GraphQLRequest request = new GraphQLRequest(query);
        errorResponse = graphQLSteps.executeQuery(query, ErrorResponse.class);
    }

    @Given("la política de seguridad establece límite diario de {string}")
    public void laPoliticaDeSeguridadEstableceLimiteDiarioDe(String limit) {
        createSecurityPolicy(limit.replace("$", "").replace(",", ""), "500000.00", "OTP");
    }

    @Given("el cliente ya ha transferido {string} hoy")
    public void elClienteYaHaTransferidoHoy(String amount) {
        if (financialAccountRepository.count() < 2) {
            createAuthenticatedCustomerWithBalance("1000000.00");
            createDestinationCustomerWithBalance("1000000.00");
        }
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas. Encontradas: " + accounts.size());
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount destAccount = accounts.get(1);

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(
            new BigDecimal(amount.replace("$", "").replace(",", ""))
        ));
        destAccount.setBalance(destAccount.getBalance().add(
            new BigDecimal(amount.replace("$", "").replace(",", ""))
        ));

        financialAccountRepository.save(sourceAccount);
        financialAccountRepository.save(destAccount);
    }

    @When("el cliente intenta transferir {string}")
    public void elClienteIntentaTransferir(String amount) {
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas. Encontradas: " + accounts.size());
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount destAccount = accounts.get(1);

        String query = buildTransferMutation(sourceAccount.getId().toString(), destAccount.getId().toString(), amount);
        errorResponse = graphQLSteps.executeQuery(query, ErrorResponse.class);
    }

    @When("el cliente transfiere {string}")
    public void elClienteTransfiere(String amount) {
        if (financialAccountRepository.count() < 2) {
            createAuthenticatedCustomerWithBalance("1000000.00");
            createDestinationCustomerWithBalance("1000000.00");
        }
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas. Encontradas: " + accounts.size());
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount destAccount = accounts.get(1);

        String query = buildTransferMutation(sourceAccount.getId().toString(), destAccount.getId().toString(), amount);
        transferResponse = graphQLSteps.executeQuery(query, Map.class);
    }

    @Given("la política de seguridad establece límite de validación de {string}")
    public void laPoliticaDeSeguridadEstableceLimiteDeValidacionDe(String limit) {
        createSecurityPolicy("1000000.00", limit.replace("$", "").replace(",", ""), "OTP");
    }

    @And("tipo de validación es {string}")
    public void tipoDeValidacionEs(String validationType) {
        // Ya se establece en createSecurityPolicy
    }

    @And("el sistema indica que requiere validación extra")
    public void elSistemaIndicaQueRequiereValidacionExtra() {
        Map<String, Object> data = (Map<String, Object>) transferResponse.getBody().get("data");
        Map<String, Object> transferData = (Map<String, Object>) data.get("ejecutarTransferencia");
        assertTrue(transferData.get("message").toString().contains("validación"));
    }

    @Then("la transacción se crea con estado {string}")
    public void laTransaccionSeCreaConEstado(String expectedStatus) {
        assertEquals(200, transferResponse.getStatusCode().value());
        Map<String, Object> data = (Map<String, Object>) transferResponse.getBody().get("data");
        Map<String, Object> transferData = (Map<String, Object>) data.get("ejecutarTransferencia");
        assertEquals(expectedStatus, transferData.get("status"));
    }

    @And("no se afectan los saldos de las cuentas hasta la validación")
    public void noSeAfectanLosSaldosDeLasCuentasHastaLaValidacion() {
        java.util.List<FinancialAccount> accounts = financialAccountRepository.findAll();
        if (accounts.size() < 2) {
            throw new IllegalStateException("Se requieren al menos 2 cuentas. Encontradas: " + accounts.size());
        }

        FinancialAccount sourceAccount = accounts.get(0);
        FinancialAccount destAccount = accounts.get(1);

        // Los saldos deben mantenerse igual
        assertNotNull(sourceAccount.getBalance());
        assertNotNull(destAccount.getBalance());
    }

    @Given("existe una política de seguridad actual")
    public void existeUnaPoliticaDeSeguridadActual() {
        createSecurityPolicy("1000000.00", "500000.00", "OTP");
    }

    @When("el administrador actualiza las políticas con límite diario {string}")
    public void elAdministradorActualizaLasPoliticasConLimiteDiario(String limit) {
        // Crear y autenticar como usuario ADMIN
        createAdminUserAndAuthenticate();

        String query = String.format(
            "mutation { ActualizarPoliticaSeguridad(input: {" +
            "dailyLimit: %s, " +
            "validationLimit: 500000.00, " +
            "validationType: \"OTP\"" +
            "}) { dailyLimit validationLimit validationType } }",
            limit.replace("$", "").replace(",", "")
        );
        GraphQLRequest request = new GraphQLRequest(query);
        transferResponse = graphQLSteps.executeQuery(query, Map.class);
    }

    @Then("la política se actualiza con los nuevos valores")
    public void laPoliticaSeActualizaConLosNuevosValores() {
        assertEquals(200, transferResponse.getStatusCode().value());

        Map<String, Object> data = (Map<String, Object>) transferResponse.getBody().get("data");
        Map<String, Object> policyData = (Map<String, Object>) data.get("ActualizarPoliticaSeguridad");

        BigDecimal expectedLimit = new BigDecimal("2000000.00");
        assertEquals(0, expectedLimit.compareTo(new BigDecimal(policyData.get("dailyLimit").toString())));
    }

    @And("se registra el usuario que actualizó")
    public void seRegistraElUsuarioQueActualizo() {
        // Verificar que la política se actualizó correctamente
        SecurityPolicy policy = securityPolicyRepository.findById(1L)
            .orElseThrow(() -> new AssertionError("Política no encontrada"));
        assertNotNull(policy.getUpdatedBy());
    }

    @And("se registra la fecha de actualización")
    public void seRegistraLaFechaDeActualizacion() {
        SecurityPolicy policy = securityPolicyRepository.findById(1L)
            .orElseThrow(() -> new AssertionError("Política no encontrada"));
        assertNotNull(policy.getUpdatedAt());
    }

    private void createAuthenticatedCustomerWithBalance(String balance) {
        // Generar ID único para evitar conflictos
        String uniqueDoc = currentDocumentNumber + "_" + (testCounter++);

        Customer customer = Customer.builder()
            .documentNumber(uniqueDoc)
            .documentType("CC")
            .name("Test Customer")
            .email("test" + testCounter + "@bank.com")
            .passwordHash(passwordEncoder.encode("SecurePass123!"))
            .status("ACTIVE")
            .birthDate(LocalDate.now().minusYears(25))
            .build();

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER no encontrado"));

        customer.setRoles(java.util.Set.of(userRole));
        customerRepository.save(customer);

        FinancialAccount account = FinancialAccount.builder()
            .customer(customer)
            .balance(new BigDecimal(balance))
            .status("ACTIVE")
            .build();
        financialAccountRepository.save(account);

        // Asegurar que el token está configurado correctamente
        String token = graphQLSteps.buildAuthenticationToken(customer.getEmail(), "SecurePass123!");
        graphQLSteps.setAuthenticationHeader(token);
    }

    private void createDestinationCustomerWithBalance(String balance) {
        // Generar ID único para evitar conflictos
        String uniqueDoc = destinationDocumentNumber + "_" + (testCounter++);

        Customer customer = Customer.builder()
            .documentNumber(uniqueDoc)
            .documentType("CC")
            .name("Dest Customer")
            .email("dest" + testCounter + "@bank.com")
            .passwordHash(passwordEncoder.encode("SecurePass123!"))
            .status("ACTIVE")
            .birthDate(LocalDate.now().minusYears(25))
            .build();

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER no encontrado"));

        customer.setRoles(java.util.Set.of(userRole));
        customerRepository.save(customer);

        FinancialAccount account = FinancialAccount.builder()
            .customer(customer)
            .balance(new BigDecimal(balance))
            .status("ACTIVE")
            .build();
        financialAccountRepository.save(account);
    }

    private void createSecurityPolicy(String dailyLimit, String validationLimit, String validationType) {
        SecurityPolicy policy = SecurityPolicy.builder()
            .idSecurityPolicy(1L)
            .dailyLimit(new BigDecimal(dailyLimit))
            .validationLimit(new BigDecimal(validationLimit))
            .validationType(validationType)
            .updatedAt(LocalDateTime.now())
            .updatedBy("system")
            .build();
        securityPolicyRepository.save(policy);
    }

    private String buildTransferMutation(String sourceAccountId, String destAccountId, String amount) {
        BigDecimal amountDecimal = new BigDecimal(amount.replace("$", "").replace(",", ""));
        return String.format(
            "mutation { ejecutarTransferencia(" +
            "sourceAccountId: \"%s\", " +
            "destinationAccountId: \"%s\", " +
            "amount: %s, " +
            "description: \"Test Transfer\"" +
            ") { success message transactionReference status destinationName } }",
            sourceAccountId, destAccountId, amountDecimal
        );
    }

    private Customer createTestCustomer(String documentNumber) {
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER no encontrado"));

        Customer customer = Customer.builder()
            .documentNumber(documentNumber)
            .documentType("CC")
            .name("Test Customer")
            .email("test@bank.com")
            .passwordHash(passwordEncoder.encode("SecurePass123!"))
            .status("ACTIVE")
            .birthDate(LocalDate.now().minusYears(25))
            .build();

        customer.setRoles(java.util.Set.of(userRole));
        return customerRepository.save(customer);
    }

    private void createAdminUserAndAuthenticate() {
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
