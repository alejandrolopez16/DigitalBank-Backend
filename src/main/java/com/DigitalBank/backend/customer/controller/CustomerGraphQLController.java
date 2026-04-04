package com.DigitalBank.backend.customer.controller;

import org.springframework.stereotype.Controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
<<<<<<< HEAD
import org.springframework.security.access.prepost.PreAuthorize;
=======
import org.springframework.graphql.data.method.annotation.SchemaMapping;
>>>>>>> 0fe90907a27b7025670cda6d4ffddf1e85a7a613

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.service.FinancialAccountService;
import com.DigitalBank.backend.customer.entity.AuthResponse;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@Controller
public class CustomerGraphQLController {

    private final CustomerService customerService;
    private final FinancialAccountService financialAccountService;

    public CustomerGraphQLController(CustomerService customerService, FinancialAccountService financialAccountService) {
        this.customerService = customerService;
        this.financialAccountService = financialAccountService;
    }

    // El nombre de este método DEBE ser igual al del schema.graphqls
    @MutationMapping
    public Customer registrarCliente(@Argument("cliente") Customer newCustomer) {
        // Llamamos a la lógica que ya habías programado
        return customerService.regiCustomer(newCustomer);
    }

    // Un query de prueba para que GraphQL no se queje
    @QueryMapping
    public String ping() {
        return "¡GraphQL está funcionando perfectamente!";
    }

    @QueryMapping
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN')") // Solo los ADMIN pueden ver esta lista
=======
    @PreAuthorize("hasRole('ADMIN')")
>>>>>>> 0fe90907a27b7025670cda6d4ffddf1e85a7a613
    public List<Customer> clientesPendientes() {
        return customerService.getPendingCustomers(); 
    }
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Customer aprobarCliente(@Argument String documentNumber) {
        return customerService.approveCustomer(documentNumber);
    }

<<<<<<< HEAD
        @MutationMapping
=======
    @MutationMapping
>>>>>>> 0fe90907a27b7025670cda6d4ffddf1e85a7a613
    @PreAuthorize("hasRole('ADMIN')")
    public Customer rechazarCliente(@Argument String documentNumber,
                                    @Argument String comentario) {
        return customerService.rejectCustomer(documentNumber, comentario);
    }

    @MutationMapping
    public AuthResponse login(@Argument String email, @Argument String passwordHash) { 
        return customerService.login(email, passwordHash);
    }

    @MutationMapping
    public FinancialAccount crearCuentaFinanciera(@Argument String documentNumber) {
        return financialAccountService.createFinancialAccount(documentNumber);
    }

    @QueryMapping
    public List<FinancialAccount> misCuentasFinancieras() {
        return financialAccountService.getMyFinancialAccounts();
    }

    @QueryMapping
    public FinancialAccount miCuentaFinanciera(@Argument String accountId) {
        return financialAccountService.getMyFinancialAccountById(UUID.fromString(accountId));
    }

    @SchemaMapping(typeName = "FinancialAccount", field = "documentNumber")
    public String documentNumber(FinancialAccount financialAccount) {
        return financialAccount.getCustomer().getDocumentNumber();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')") // ADMIN y USER pueden acceder
    public Customer clientePorDocumento(@Argument String documentNumber) {
        return customerService.getCustomerByDocument(documentNumber);
    }

    @MutationMapping
    public FinancialAccount bloquearCuenta(@Argument String accountId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return financialAccountService.blockAccount(UUID.fromString(accountId), email);
    }

    @MutationMapping
    public FinancialAccount desbloquearCuenta(@Argument String accountId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return financialAccountService.unblockAccount(UUID.fromString(accountId), email);
    }
}