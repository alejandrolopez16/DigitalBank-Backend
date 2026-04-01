package com.DigitalBank.backend.customer.controller;

import org.springframework.stereotype.Controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.service.FinancialAccountService;
import com.DigitalBank.backend.customer.entity.AuthResponse;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.service.CustomerService;

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
    public List<Customer> clientesPendientes() {
        return customerService.getPendingCustomers(); 
    }
    @MutationMapping
    public Customer aprobarCliente(@Argument String documentNumber) {
        return customerService.approveCustomer(documentNumber);
    }

        @MutationMapping
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
    public Customer clientePorDocumento(@Argument String documentNumber) {
        return customerService.getCustomerByDocument(documentNumber);
}
}