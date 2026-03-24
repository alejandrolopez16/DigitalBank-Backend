package com.DigitalBank.backend.customer.controller;

import org.springframework.stereotype.Controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.service.CustomerService;// Ojo con estos imports, que coincidan con tus carpetas

@Controller
public class CustomerGraphQLController {

    private final CustomerService customerService;

    public CustomerGraphQLController(CustomerService customerService) {
        this.customerService = customerService;
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

    @QueryMapping
    public Customer clientePorDocumento(@Argument String documentNumber) {
        return customerService.getCustomerByDocument(documentNumber);
}
}