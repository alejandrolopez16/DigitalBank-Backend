package com.DigitalBank.backend.customer.service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.DigitalBank.backend.customer.entity.AuthResponse;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.customer.repository.RoleRepository;
import com.DigitalBank.backend.email.service.EmailService;
import com.DigitalBank.backend.email.templates.EmailTemplates;
import com.DigitalBank.backend.config.JwtUtil;
import com.DigitalBank.backend.customer.entity.Role;


import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;



@Service

public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RoleRepository roleRepository;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder, EmailService emailService, JwtUtil jwtUtil, RoleRepository roleRepository) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder; 
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.roleRepository = roleRepository;
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
    Role userRole=roleRepository.findByName("ROLE_USER")
        .orElseThrow(()-> new RuntimeException("Error, el rol USER no se encuentra en la base de datos"));
    newCustomer.setStatus("PENDING");
    String passwordEncripted=passwordEncoder.encode(newCustomer.getPasswordHash());
    newCustomer.setPasswordHash(passwordEncripted);
    newCustomer.setRoles(java.util.Set.of(userRole));

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
        .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
}

public AuthResponse login(String email, String password) {
    //Validación de email y contraseña para logueo
    Customer customer = customerRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Correo o contraseña incorrectos"));

    //Validación de cuenta bloqueada por intentos fallidos
    if (customer.getLockTime() != null && customer.getLockTime().plusMinutes(10).isAfter(LocalDateTime.now())) {
        throw new IllegalArgumentException("Cuenta bloqueada por intentos fallidos. Intente nuevamente en 10 minutos.");
    }
    //Validación de contraseña
    if (!passwordEncoder.matches(password, customer.getPasswordHash())) {
        int attempts = customer.getLoginAttempts() == null ?0 :customer.getLoginAttempts() ;
        attempts++;
        customer.setLoginAttempts(attempts);

        if (attempts >= 3) {
            customer.setLockTime(LocalDateTime.now());
            customerRepository.save(customer);
            throw new IllegalArgumentException("Cuenta bloqueada por intentos fallidos. Intente nuevamente en 10 minutos.");
        }

        customerRepository.save(customer);
        throw new IllegalArgumentException("Contraseña incorrecta");
    }

    //Si el login es exitoso, se resetean los intentos fallidos y se genera el token JWT

    customer.setLoginAttempts(0);
    customer.setLockTime(null);
    customerRepository.save(customer);

    String roles=customer.getRoles().stream().map(role->role.getName()).collect(Collectors.joining(","));
    String token = jwtUtil.generateToken(customer.getEmail(),roles);

    return AuthResponse.builder()
        .token(token)
        .status(customer.getStatus())
        .role(roles)
        .message("Inicio de sesión exitoso")
        .build();
}
}
