package com.DigitalBank.backend.customer.service;


import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.email.service.EmailService;
import com.DigitalBank.backend.email.templates.EmailTemplates;


import java.util.List;



@Service

public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder; 
        this.emailService = emailService;

    }
public Customer regiCustomer(Customer newCustomer){

    int age=Period.between(newCustomer.getBirthDate(), LocalDate.now()).getYears();
    if(age<18){
        throw new IllegalArgumentException("Error,el cliente debe ser mayor de 18 años para registrarse");
    }
    if(customerRepository.existsByEmail(newCustomer.getEmail())){
        throw new IllegalArgumentException("Error, el correo electrónico ya está registrado");
    }
    if (customerRepository.existsByDocumentNumber(newCustomer.getDocumentNumber())){
        throw new IllegalArgumentException("Error, el número de documento ya está registrado");
    }
    newCustomer.setStatus("PENDING");
    String passwordEncripted=passwordEncoder.encode(newCustomer.getPasswordHash());
    newCustomer.setPasswordHash(passwordEncripted);

    return customerRepository.save(newCustomer);
}

public List<Customer> getPendingCustomers() {
    return customerRepository.findByStatus("PENDING");
}

public Customer approveCustomer(String documentNumber) {

    Customer customer = customerRepository.findById(documentNumber)
        .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

    customer.setStatus("ACTIVE");
    customer.setRejectionComments(null);

    String html = EmailTemplates.approvedTemplate(customer.getName());

    emailService.sendEmail(
        customer.getEmail(),
        "Cuenta aprobada",
        html
    );
    return customerRepository.save(customer);
}

public Customer rejectCustomer(String documentNumber, String comment) {

    if (comment == null || comment.trim().isEmpty()) {
    throw new IllegalArgumentException("El comentario es obligatorio para rechazar");
}
    Customer customer = customerRepository.findById(documentNumber)
        .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

    customer.setStatus("REJECTED");
    customer.setRejectionComments(comment);

    String html = EmailTemplates.rejectedTemplate(
        customer.getName(),
        comment
    );

    emailService.sendEmail(
        customer.getEmail(),
        "Cuenta rechazada",
        html
    );  
    

    return customerRepository.save(customer);
}

public Customer getCustomerByDocument(String documentNumber) {
    return customerRepository.findById(documentNumber)
        .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
}
    private final EmailService emailService;




}
