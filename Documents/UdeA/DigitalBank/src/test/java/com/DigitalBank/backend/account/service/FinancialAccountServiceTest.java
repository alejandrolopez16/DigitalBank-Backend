package com.DigitalBank.backend.account.service;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
