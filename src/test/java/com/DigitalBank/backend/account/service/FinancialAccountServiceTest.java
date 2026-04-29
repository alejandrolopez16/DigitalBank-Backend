package com.DigitalBank.backend.account.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class FinancialAccountServiceTest {

    @Mock
    private FinancialAccountRepository financialAccountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private FinancialAccountService financialAccountService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // Pruebas para el método createFinancialAccount | HU-1.2.1: Crear una cuenta financiera ↓
    
    // Test: Validación de que un usuario autenticado no pueda crear una cuenta financiera para otro número de documento.   
    @Test
    void shouldRejectWhenAuthenticatedUserDoesNotOwnDocumentNumber() {
        Customer authenticatedCustomer = Customer.builder()
                .documentNumber("111")
                .email("owner@bank.com")
                .build();

        setAuthenticatedUser("owner@bank.com");
        when(customerRepository.findByEmail("owner@bank.com")).thenReturn(Optional.of(authenticatedCustomer));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> financialAccountService.createFinancialAccount("999"));

        assertEquals("Solo puedes crear una cuenta financiera para tu propio usuario", exception.getMessage());
    }

    // Test: Validación de que un usuario autenticado pueda crear una cuenta financiera para su propio número de documento exitosamente
    // y que el balance inicial sea 0.
    @Test
    void shouldCreateAccountWhenAuthenticatedUserOwnsDocumentNumber() {
        Customer authenticatedCustomer = Customer.builder()
                .documentNumber("111")
                .email("owner@bank.com")
                .build();

        setAuthenticatedUser("owner@bank.com");
        when(customerRepository.findByEmail("owner@bank.com")).thenReturn(Optional.of(authenticatedCustomer));
        when(customerRepository.findById("111")).thenReturn(Optional.of(authenticatedCustomer));
        when(financialAccountRepository.save(any(FinancialAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialAccount account = financialAccountService.createFinancialAccount("111");

        assertEquals(BigDecimal.ZERO, account.getBalance());
        assertEquals("111", account.getCustomer().getDocumentNumber());
        verify(financialAccountRepository).save(any(FinancialAccount.class));
    }

    // Test: Validación de que un usuario no autenticado no pueda crear una cuenta financiera.
    @Test
    void shouldRejectCreateAccountWhenAuthenticationIsMissing() {
        SecurityContextHolder.clearContext();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> financialAccountService.createFinancialAccount("111"));

        assertEquals("Debe iniciar sesión con un token JWT válido para consultar o crear cuentas financieras", exception.getMessage());
    }

    // Pruebas para el método getMyFinancialAccountById | HU-1.2.2: Consultar información de una cuenta ↓

    // Test: Validación de que un usuario autenticado no pueda consultar una cuenta financiera que no le pertenece.
    @Test
    void shouldRejectAccountByIdWhenItDoesNotBelongToAuthenticatedUser() {
        Customer authenticatedCustomer = Customer.builder()
                .documentNumber("111")
                .email("owner@bank.com")
                .build();
        UUID accountId = UUID.randomUUID();

        setAuthenticatedUser("owner@bank.com");
        when(customerRepository.findByEmail("owner@bank.com")).thenReturn(Optional.of(authenticatedCustomer));
        when(financialAccountRepository.findByIdAndCustomerDocumentNumber(eq(accountId), eq("111")))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> financialAccountService.getMyFinancialAccountById(accountId));

        assertEquals("Cuenta financiera no encontrada para el usuario autenticado", exception.getMessage());
    }

    // Test: Validación de que un usuario autenticado pueda consultar una cuenta financiera que le pertenece exitosamente.
    @Test
    void shouldReturnAccountByIdForAuthenticatedOwner() {
        Customer authenticatedCustomer = Customer.builder()
                .documentNumber("111")
                .email("owner@bank.com")
                .build();
        UUID accountId = UUID.randomUUID();

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .customer(authenticatedCustomer)
                .balance(BigDecimal.TEN)
                .build();

        setAuthenticatedUser("owner@bank.com");
        when(customerRepository.findByEmail("owner@bank.com")).thenReturn(Optional.of(authenticatedCustomer));
        when(financialAccountRepository.findByIdAndCustomerDocumentNumber(accountId, "111"))
                .thenReturn(Optional.of(account));

        FinancialAccount result = financialAccountService.getMyFinancialAccountById(accountId);

        assertEquals(accountId, result.getId());
        assertEquals(BigDecimal.TEN, result.getBalance());
    }

    // Pruebas para el método getMyFinancialAccounts | HU-1.2.3:  Listar cuentas de un cliente ↓

    // Test: Validación de que un usuario autenticado solo pueda consultar las cuentas financieras que le pertenecen.
    @Test
    void shouldReturnOnlyAuthenticatedUserAccounts() {
        Customer authenticatedCustomer = Customer.builder()
                .documentNumber("111")
                .email("owner@bank.com")
                .build();

        FinancialAccount first = FinancialAccount.builder()
                .id(UUID.randomUUID())
                .customer(authenticatedCustomer)
                .balance(BigDecimal.ZERO)
                .build();

        FinancialAccount second = FinancialAccount.builder()
                .id(UUID.randomUUID())
                .customer(authenticatedCustomer)
                .balance(BigDecimal.TEN)
                .build();

        setAuthenticatedUser("owner@bank.com");
        when(customerRepository.findByEmail("owner@bank.com")).thenReturn(Optional.of(authenticatedCustomer));
        when(financialAccountRepository.findByCustomerDocumentNumber("111")).thenReturn(List.of(first, second));

        List<FinancialAccount> accounts = financialAccountService.getMyFinancialAccounts();

        assertEquals(2, accounts.size());
        assertEquals("111", accounts.get(0).getCustomer().getDocumentNumber());
        verify(financialAccountRepository).findByCustomerDocumentNumber("111");
    }

    // Pruebas para el método blockAccount | HU-1.2.4: Bloquear una cuenta financiera ↓

    // Test: Validación de que un usuario autenticado pueda bloquear una cuenta financiera que le pertenece.
    @Test
    void shouldBlockAccountWhenAuthenticatedUserIsOwner() {
        Customer authenticatedCustomer = Customer.builder()
                .documentNumber("111")
                .email("owner@bank.com")
                .build();
        UUID accountId = UUID.randomUUID();

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .customer(authenticatedCustomer)
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .build();

        setAuthenticatedUser("owner@bank.com");
        when(customerRepository.findByEmail("owner@bank.com")).thenReturn(Optional.of(authenticatedCustomer));
        when(financialAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(financialAccountRepository.save(any(FinancialAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialAccount blocked = financialAccountService.blockAccount(accountId, "owner@bank.com");

        assertEquals("BLOCKED", blocked.getStatus());
        verify(financialAccountRepository).save(account);
    }


    // Test: Validación de que un usuario autenticado no pueda bloquear una cuenta financiera que no le pertenece.
    @Test
    void shouldRejectBlockAccountWhenAuthenticatedUserIsNotOwner() {
        Customer authenticatedCustomer = Customer.builder()
                .documentNumber("111")
                .email("owner@bank.com")
                .build();
        Customer otherCustomer = Customer.builder()
                .documentNumber("999")
                .email("other@bank.com")
                .build();
        UUID accountId = UUID.randomUUID();

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .customer(otherCustomer)
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .build();

        setAuthenticatedUser("owner@bank.com");
        when(customerRepository.findByEmail("owner@bank.com")).thenReturn(Optional.of(authenticatedCustomer));
        when(financialAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> financialAccountService.blockAccount(accountId, "owner@bank.com"));

        assertEquals("No autorizado", exception.getMessage());
    }

    // Test: Validación de que un usuario autenticado pueda desbloquear una cuenta financiera que le pertenece.
    @Test
    void shouldUnblockAccountWhenAuthenticatedUserIsOwner() {
        Customer authenticatedCustomer = Customer.builder()
                .documentNumber("111")
                .email("owner@bank.com")
                .build();
        UUID accountId = UUID.randomUUID();

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .customer(authenticatedCustomer)
                .balance(BigDecimal.ZERO)
                .status("BLOCKED")
                .build();

        setAuthenticatedUser("owner@bank.com");
        when(customerRepository.findByEmail("owner@bank.com")).thenReturn(Optional.of(authenticatedCustomer));
        when(financialAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(financialAccountRepository.save(any(FinancialAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialAccount unblocked = financialAccountService.unblockAccount(accountId, "owner@bank.com");

        assertEquals("ACTIVE", unblocked.getStatus());
        assertNotNull(unblocked);
        verify(financialAccountRepository).save(account);
    }

    // Metodo auxiliar para configurar el usuario autenticado en el contexto de seguridad para las pruebas
    private void setAuthenticatedUser(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
        SecurityContextHolder.setContext(context);
    }
}
