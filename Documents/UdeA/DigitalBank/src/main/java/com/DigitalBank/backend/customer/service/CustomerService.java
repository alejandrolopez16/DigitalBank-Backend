package com.DigitalBank.backend.customer.service;


import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;


@Service

public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder; 
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
}
