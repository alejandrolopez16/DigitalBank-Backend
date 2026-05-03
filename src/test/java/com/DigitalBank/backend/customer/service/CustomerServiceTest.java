package com.DigitalBank.backend.customer.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.DigitalBank.backend.config.JwtUtil;
import com.DigitalBank.backend.customer.entity.AuthResponse;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.entity.Role;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.customer.repository.RoleRepository;
import com.DigitalBank.backend.email.service.EmailService;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private CustomerService customerService;

    // Pruebas para el método regiCustomer | HU-1.1.1: Registro de prospecto de cliente  ↓

    // Test: Validación de que el cliente sea mayor de edad (18 años o más) para registrarse.
    @Test
    void shouldRejectRegistrationWhenCustomerIsUnderage() {
        Customer customer = baseCustomer();
        customer.setBirthDate(LocalDate.now().minusYears(17));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.regiCustomer(customer));

        assertEquals("Error,el cliente debe ser mayor de 18 años para registrarse", exception.getMessage());
    }

    // Test: Validación de que el correo electrónico no esté registrado.
    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        Customer customer = baseCustomer();

        when(customerRepository.existsByEmail("ana@bank.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.regiCustomer(customer));

        assertEquals("Error, el correo electrónico ya está registrado", exception.getMessage());
    }

    // Test: Validación de que el número de documento no esté registrado.
    @Test
    void shouldRejectRegistrationWhenDocumentAlreadyExists() {
        Customer customer = baseCustomer();

        when(customerRepository.existsByEmail("ana@bank.com")).thenReturn(false);
        when(customerRepository.existsByDocumentNumber("123456")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.regiCustomer(customer));

        assertEquals("Error, el número de documento ya está registrado", exception.getMessage());
    }

    // Test: Encriptación de la contraseña y establecimiento del estado PENDING al registrar un cliente exitosamente.
    @Test
    void shouldEncryptPasswordAndSetPendingStatusOnRegistration() {
        Customer customer = baseCustomer();
        Role userRole = new Role(1, "ROLE_USER", "Usuario de plataforma");

        when(customerRepository.existsByEmail("ana@bank.com")).thenReturn(false);
        when(customerRepository.existsByDocumentNumber("123456")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer registered = customerService.regiCustomer(customer);

        assertEquals("PENDING", registered.getStatus());
        assertEquals("encoded-password", registered.getPasswordHash());
        assertEquals(1, registered.getRoles().size());
        assertEquals("ROLE_USER", registered.getRoles().iterator().next().getName());
        verify(customerRepository).save(registered);
    }

    // Pruebas para el método getPendingCustomers | HU-1.1.2: Gestión de Validación de Identidad  ↓

    // Test: Verificación de que solo se devuelvan clientes con estado PENDING.
    @Test 
    void shouldListOnlyPendingCustomers() {
        List<Customer> pending = List.of(baseCustomer(), baseCustomer());
        when(customerRepository.findByStatus("PENDING")).thenReturn(pending);

        List<Customer> result = customerService.getPendingCustomers();

        assertEquals(2, result.size());
        verify(customerRepository).findByStatus("PENDING");
    }

    // Test: Aprobación de la validación del usuario y envío de correo electrónico de aprobación.
    @Test
    void shouldApproveCustomerAndSendApprovalEmail() {
        Customer customer = baseCustomer();
        customer.setStatus("PENDING");
        customer.setRejectionComments("Falta documento");

        when(customerRepository.findById("123456")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer approved = customerService.approveCustomer("123456");

        assertEquals("ACTIVE", approved.getStatus());
        assertNull(approved.getRejectionComments());
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
        verify(customerRepository).save(customer);
    }

    // Test: Rechazo de la validación del usuario con razón y envío de correo electrónico de rechazo.
    @Test
    void shouldRejectCustomerWithCommentAndSendRejectionEmail() {
        Customer customer = baseCustomer();
        customer.setStatus("PENDING");

        when(customerRepository.findById("123456")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer rejected = customerService.rejectCustomer("123456", "Documento borroso");

        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("Documento borroso", rejected.getRejectionComments());
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
        verify(customerRepository).save(customer);
    }

    // Test: Validación de que el comentario sea obligatorio para rechazar un cliente.
    @Test
    void shouldRejectRejectionWhenCommentIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.rejectCustomer("123456", "   "));

        assertEquals("El comentario es obligatorio para rechazar", exception.getMessage());
        verify(customerRepository, never()).findById(anyString());
    }

    // Pruebas para el método login | HU-1.1.3:  Autenticación y Control de Acceso  ↓

    // Test: Validación de que la contraseña incorrecta incremente el contador de intentos fallidos.
    @Test
    void shouldIncrementAttemptsOnWrongPassword() {
        Customer customer = baseCustomer();
        customer.setLoginAttempts(0);

        when(customerRepository.findByEmail("ana@bank.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "raw-password")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.login("ana@bank.com", "wrong"));

        assertEquals("Contraseña incorrecta", exception.getMessage());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getLoginAttempts());
        assertNull(captor.getValue().getLockTime());
    }

    // Test: Validación de que después de 3 intentos fallidos, la cuenta se bloquee por 10 minutos.
    @Test
    void shouldLockAccountAfterThirdFailedLogin() {
        Customer customer = baseCustomer();
        customer.setLoginAttempts(2);

        when(customerRepository.findByEmail("ana@bank.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "raw-password")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.login("ana@bank.com", "wrong"));

        assertEquals("Cuenta bloqueada por intentos fallidos. Intente nuevamente en 10 minutos.", exception.getMessage());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getLoginAttempts());
        assertNotNull(captor.getValue().getLockTime());
    }

    // Test: Validación de que una cuenta bloqueada no permita el inicio de sesión hasta que pasen los 10 minutos.
    @Test
    void shouldRejectLoginWhenAccountIsStillLocked() {
        Customer customer = baseCustomer();
        customer.setLockTime(LocalDateTime.now().minusMinutes(5));

        when(customerRepository.findByEmail("ana@bank.com")).thenReturn(Optional.of(customer));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.login("ana@bank.com", "raw-password"));

        assertEquals("Cuenta bloqueada por intentos fallidos. Intente nuevamente en 10 minutos.", exception.getMessage());
    }

    // Test: Validación de que una cuenta bloqueada permita el inicio de sesión después de que pasen los 10 minutos, que
    //  se reseteen los intentos fallidos y se genere el token JWT correctamente.
    @Test
    void shouldLoginSuccessfullyAndResetAttempts() {
        Customer customer = baseCustomer();
        customer.setStatus("ACTIVE");
        customer.setLoginAttempts(2);
        customer.setLockTime(LocalDateTime.now().minusMinutes(20));
        customer.setRoles(Set.of(new Role(1, "ROLE_USER", "Usuario")));

        when(customerRepository.findByEmail("ana@bank.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("raw-password", "raw-password")).thenReturn(true);
        when(jwtUtil.generateToken("ana@bank.com", "ROLE_USER")).thenReturn("jwt-token");
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = customerService.login("ana@bank.com", "raw-password");

        assertEquals("jwt-token", response.getToken());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("ROLE_USER", response.getRole());
        assertEquals("Inicio de sesión exitoso", response.getMessage());
        assertEquals(0, customer.getLoginAttempts());
        assertNull(customer.getLockTime());
    }

    // Método auxiliar para crear un cliente base para las pruebas
    private Customer baseCustomer() {
        return Customer.builder()
                .documentNumber("123456")
                .documentType("CC")
                .name("Ana Lopez")
                .birthDate(LocalDate.now().minusYears(25))
                .email("ana@bank.com")
                .passwordHash("raw-password")
                .phone("3000000000")
                .address("Calle 10")
                .status("PENDING")
                .build();
    }
}
