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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("Debe iniciar sesión con un token JWT válido para crear una cuenta financiera");
        }

        String authenticatedEmail = authentication.getName();
        Customer authenticatedCustomer = customerRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));

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
}
