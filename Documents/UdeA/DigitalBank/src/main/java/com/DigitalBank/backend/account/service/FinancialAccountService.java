package com.DigitalBank.backend.account.service;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class FinancialAccountService {

    private final FinancialAccountRepository financialAccountRepository;
    private final CustomerRepository customerRepository;

    public FinancialAccountService(FinancialAccountRepository financialAccountRepository,
                                   CustomerRepository customerRepository) {
        this.financialAccountRepository = financialAccountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public FinancialAccount createFinancialAccount(String documentNumber) {
        Customer authenticatedCustomer = getAuthenticatedCustomer();

        if (!authenticatedCustomer.getDocumentNumber().equals(documentNumber)) {
            throw new IllegalArgumentException("Solo puedes crear una cuenta financiera para tu propio usuario");
        }

        Customer customer = customerRepository.findById(documentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        FinancialAccount account = FinancialAccount.builder()
                .customer(customer)
                .balance(BigDecimal.ZERO)
                .build();

        return financialAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<FinancialAccount> getMyFinancialAccounts() {
        Customer authenticatedCustomer = getAuthenticatedCustomer();
        return financialAccountRepository.findByCustomerDocumentNumber(authenticatedCustomer.getDocumentNumber());
    }

    @Transactional(readOnly = true)
    public FinancialAccount getMyFinancialAccountById(UUID accountId) {
        Customer authenticatedCustomer = getAuthenticatedCustomer();
        return financialAccountRepository.findByIdAndCustomerDocumentNumber(accountId, authenticatedCustomer.getDocumentNumber())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta financiera no encontrada para el usuario autenticado"));
    }

    private Customer getAuthenticatedCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("Debe iniciar sesión con un token JWT válido para consultar o crear cuentas financieras");
        }

        String authenticatedEmail = authentication.getName();
        return customerRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    public FinancialAccount blockAccount(UUID accountId, String email) {

        Customer customer = getAuthenticatedCustomer();

        FinancialAccount account = financialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        // Validar que sea el dueño
        if (!account.getCustomer().getEmail().equals(customer.getEmail())) {
            throw new RuntimeException("No autorizado");
        }

        account.setStatus("BLOCKED");
        return financialAccountRepository.save(account);
    }

    public FinancialAccount unblockAccount(UUID accountId, String email) {
        Customer customer = getAuthenticatedCustomer();

        FinancialAccount account = financialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        if (!account.getCustomer().getEmail().equals(customer.getEmail())) {
            throw new RuntimeException("No autorizado");
        }

        account.setStatus("ACTIVE");
        return financialAccountRepository.save(account);
    }
}
