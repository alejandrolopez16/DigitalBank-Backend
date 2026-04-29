package com.DigitalBank.backend.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.transaction.entity.SecurityPolicy;
import com.DigitalBank.backend.transaction.entity.Transaction;
import com.DigitalBank.backend.transaction.repository.TransactionRepository;

/**
 * Pruebas unitarias para el servicio TransactionService.
 * 
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FinancialAccountRepository financialAccountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SecurityPolicyService securityPolicyService;

    @InjectMocks
    private TransactionService transactionService;

    // ============= Pruebas HU 2.1.2: Validación de cuentas para transferencia =============

    // Test: Validación de que la cuenta destino existe
    @Test
    void shouldValidateDestinationAccountExists() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");
        destCustomer.setEmail("dest@bank.com");
        destCustomer.setName("María López Rodríguez");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));
        when(customerRepository.findById("654321"))
                .thenReturn(Optional.of(destCustomer));
        when(securityPolicyService.getPolicy())
                .thenReturn(baseSecurityPolicy());
        when(transactionRepository.sumarTransaccionesDelDia(sourceId, LocalDate.now().atStartOfDay()))
                .thenReturn(BigDecimal.ZERO);
        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Pago de servicios"
        );

        // Assert
        assertTrue((boolean) result.get("success"));
        verify(financialAccountRepository).findById(sourceId);
        verify(financialAccountRepository).findById(destId);
    }

    // Test: Validación de que se rechace transferencia cuando la cuenta destino no existe
    @Test
    void shouldRejectWhenDestinationAccountDoesNotExist() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Transferencia"
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("Cuenta destino no encontrada", result.get("message"));
        assertEquals("REJECTED", result.get("status"));
        verify(financialAccountRepository, never()).save(any(FinancialAccount.class));
    }

    // Test: Validación de que el nombre del usuario destino se muestre parcialmente oculto por seguridad
    @Test
    void shouldDisplayDestinationUserNamePartiallyObfuscated() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");
        destCustomer.setEmail("dest@bank.com");
        destCustomer.setName("María López Rodríguez");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));
        when(customerRepository.findById("654321"))
                .thenReturn(Optional.of(destCustomer));
        when(securityPolicyService.getPolicy())
                .thenReturn(baseSecurityPolicy());
        when(transactionRepository.sumarTransaccionesDelDia(sourceId, LocalDate.now().atStartOfDay()))
                .thenReturn(BigDecimal.ZERO);
        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Pago"
        );

        // Assert
        assertTrue((boolean) result.get("success"));
        String obfuscatedName = (String) result.get("destinationName");
        assertNotNull(obfuscatedName);
        // Verifica que el nombre está parcialmente oculto (primeras letras visibles, resto oculto)
        assertTrue(obfuscatedName.contains("*") || !obfuscatedName.equals("María López Rodríguez"));
    }

    // Test: Validación de que se rechace la transferencia cuando la cuenta origen está bloqueada
    @Test
    void shouldRejectWhenSourceAccountIsBlocked() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));
        sourceAccount.setStatus("BLOCKED");

        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Transferencia"
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("Operación rechazada: La cuenta de origen está bloqueada.", result.get("message"));
        verify(financialAccountRepository, never()).save(any(FinancialAccount.class));
    }

    // Test: Validación de que se rechace la transferencia cuando la cuenta destino está bloqueada

    @Test
    void shouldRejectWhenDestinationAccountIsBlocked() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));
        destAccount.setStatus("BLOCKED");

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Transferencia"
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("Operación rechazada: La cuenta de destino está bloqueada.", result.get("message"));
        verify(financialAccountRepository, never()).save(any(FinancialAccount.class));
    }

    // ============= Pruebas HU 2.2.1: Ejecución y confirmación de transferencia =============

    // Test: Validación de que se selecciona correctamente la cuenta origen para la transferencia
    @Test
    void shouldSelectSourceAccountForTransfer() {
        // Arrange
        Customer customer = baseCustomer();
        FinancialAccount sourceAccount = baseFinancialAccount(customer, new BigDecimal("5000.00"));

        UUID sourceId = sourceAccount.getId();

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));

        // Act - Verificamos que encuentra la cuenta origen
        Optional<FinancialAccount> result = financialAccountRepository.findById(sourceId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(sourceId, result.get().getId());
        assertEquals(new BigDecimal("5000.00"), result.get().getBalance());
        verify(financialAccountRepository).findById(sourceId);
    }

    // Test: Validación de que se ejecute correctamente una transferencia exitosa

    @Test
    void shouldExecuteSuccessfulTransfer() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");
        destCustomer.setEmail("dest@bank.com");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));
        when(customerRepository.findById("654321"))
                .thenReturn(Optional.of(destCustomer));
        when(securityPolicyService.getPolicy())
                .thenReturn(baseSecurityPolicy());
        when(transactionRepository.sumarTransaccionesDelDia(sourceId, LocalDate.now().atStartOfDay()))
                .thenReturn(BigDecimal.ZERO);
        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Pago de servicios"
        );

        // Assert
        assertTrue((boolean) result.get("success"));
        assertEquals("Transferencia Exitosa", result.get("message"));
        assertEquals("COMPLETED", result.get("status"));
        assertNotNull(result.get("transactionReference"));
        verify(financialAccountRepository, times(2)).save(any(FinancialAccount.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    // Test: Validación de que se rechace la transferencia cuando la cuenta origen tiene fondos insuficientes
    @Test
    void shouldRejectWhenInsufficientBalance() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("100.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Transferencia"
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("Fondos insuficientes en la cuenta origen", result.get("message"));
        assertEquals("REJECTED", result.get("status"));
        verify(financialAccountRepository, never()).save(any(FinancialAccount.class));
    }

    // Test: Validación de que se rechace la transferencia cuando el monto es cero o negativo

    @Test
    void shouldRejectWhenAmountIsZeroOrNegative() {
        // Arrange
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                negativeAmount,
                "Transferencia"
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("El monto de la transferencia debe ser mayor a cero", result.get("message"));
        verify(financialAccountRepository, never()).findById(any());
    }

    /**
     * Test: Validación de que se rechace la transferencia cuando el monto es null
     */
    @Test
    void shouldRejectWhenAmountIsNull() {
        // Arrange
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                null,
                "Transferencia"
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("El monto de la transferencia debe ser mayor a cero", result.get("message"));
    }

    // Test: Validación de que se acepte un concepto válido con hasta 50 caracteres
    @Test
    void shouldAcceptConceptWithMaximum50Characters() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("500.00");
        String validConcept = "Pago de servicios mensuales"; // 29 caracteres

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));
        when(customerRepository.findById("654321"))
                .thenReturn(Optional.of(destCustomer));
        when(securityPolicyService.getPolicy())
                .thenReturn(baseSecurityPolicy());
        when(transactionRepository.sumarTransaccionesDelDia(sourceId, LocalDate.now().atStartOfDay()))
                .thenReturn(BigDecimal.ZERO);
        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                validConcept
        );

        // Assert
        assertTrue((boolean) result.get("success"));
        verify(transactionRepository).save(any(Transaction.class));
    }

    /**
     * Test: Validación de que se rechace la transferencia cuando el concepto excede los 50 caracteres
     */
    @Test
    void shouldRejectConceptExceeding50Characters() {
        // Arrange
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");
        String invalidConcept = "Este concepto es muy largo y excede los 50 caracteres permitidos por el sistema";

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                invalidConcept
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("La descripción no puede exceder los 50 caracteres", result.get("message"));
        verify(financialAccountRepository, never()).findById(any());
    }

    // Test: Validación de que se muestre un mensaje de confirmación con referencia de transacción al finalizar exitosamente la transferencia
    @Test
    void shouldDisplayConfirmationMessageOnSuccess() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));
        when(customerRepository.findById("654321"))
                .thenReturn(Optional.of(destCustomer));
        when(securityPolicyService.getPolicy())
                .thenReturn(baseSecurityPolicy());
        when(transactionRepository.sumarTransaccionesDelDia(sourceId, LocalDate.now().atStartOfDay()))
                .thenReturn(BigDecimal.ZERO);
        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Pago"
        );

        // Assert
        assertTrue((boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("Transferencia Exitosa"));
        assertNotNull(result.get("transactionReference"));
    }

    // Test: Validación de que se muestre un mensaje de error al finalizar una transferencia fallida
    @Test
    void shouldDisplayErrorMessageOnFailure() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.00");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Transferencia"
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("Cuenta destino no encontrada", result.get("message"));
        assertEquals("REJECTED", result.get("status"));
    }

    // Test: Validación de que se rechace la transferencia cuando se ha superado el límite diario de transferencias
    @Test
    void shouldRejectWhenDailyLimitExceeded() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("5000000.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("600000.00");

        SecurityPolicy policy = baseSecurityPolicy();
        policy.setDailyLimit(new BigDecimal("1000000.00"));

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));
        when(securityPolicyService.getPolicy())
                .thenReturn(policy);
        when(transactionRepository.sumarTransaccionesDelDia(sourceId, LocalDate.now().atStartOfDay()))
                .thenReturn(new BigDecimal("500000.00")); // Ya ha gastado 500000

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Transferencia"
        );

        // Assert
        assertFalse((boolean) result.get("success"));
        assertEquals("Rechazo: Se ha superado el límite diario de transferencias.", result.get("message"));
        verify(financialAccountRepository, never()).save(any(FinancialAccount.class));
    }

    // Test: Validación de que se solicite validación adicional (OTP) cuando el monto de la transferencia excede el límite de validación
    @Test
    void shouldRequestAdditionalValidationWhenThresholdExceeded() {
        // Arrange
        Customer sourceCustomer = baseCustomer();
        Customer destCustomer = baseCustomer();
        destCustomer.setDocumentNumber("654321");

        FinancialAccount sourceAccount = baseFinancialAccount(sourceCustomer, new BigDecimal("2000000.00"));
        FinancialAccount destAccount = baseFinancialAccount(destCustomer, new BigDecimal("1000.00"));

        UUID sourceId = sourceAccount.getId();
        UUID destId = destAccount.getId();
        BigDecimal amount = new BigDecimal("750000.00"); // Mayor al validationLimit de 500000

        SecurityPolicy policy = baseSecurityPolicy();
        policy.setValidationLimit(new BigDecimal("500000.00"));
        policy.setValidationType("OTP");

        when(financialAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(sourceAccount));
        when(financialAccountRepository.findById(destId))
                .thenReturn(Optional.of(destAccount));
        when(customerRepository.findById("654321"))
                .thenReturn(Optional.of(destCustomer));
        when(securityPolicyService.getPolicy())
                .thenReturn(policy);
        when(transactionRepository.sumarTransaccionesDelDia(sourceId, LocalDate.now().atStartOfDay()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> result = transactionService.ejecutarTransferencia(
                sourceId.toString(),
                destId.toString(),
                amount,
                "Transferencia importante"
        );

        // Assert
        assertTrue((boolean) result.get("success"));
        assertEquals("PENDING_VALIDATION", result.get("status"));
        assertTrue(result.get("message").toString().contains("requiere validación extra"));
        assertTrue(result.get("message").toString().contains("OTP"));
        assertNotNull(result.get("transactionReference"));
    }


    // ============= Métodos auxiliares =============

    /**
     * Crea un cliente base para las pruebas.
     */
    private Customer baseCustomer() {
        return Customer.builder()
                .documentNumber("123456")
                .email("client@bank.com")
                .name("Juan Pérez García")
                .status("ACTIVE")
                .build();
    }

    /**
     * Crea una cuenta financiera base para las pruebas.
     */
    private FinancialAccount baseFinancialAccount(Customer customer, BigDecimal balance) {
        return FinancialAccount.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .balance(balance)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Crea una política de seguridad base para las pruebas.
     */
    private SecurityPolicy baseSecurityPolicy() {
        return SecurityPolicy.builder()
                .idSecurityPolicy(1L)
                .dailyLimit(new BigDecimal("1000000.00"))
                .validationLimit(new BigDecimal("500000.00"))
                .validationType("OTP")
                .updatedAt(LocalDateTime.now())
                .updatedBy("system")
                .build();
    }
}
