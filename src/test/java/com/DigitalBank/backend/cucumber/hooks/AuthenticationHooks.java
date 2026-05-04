package com.DigitalBank.backend.cucumber.hooks;

import io.cucumber.java.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.DigitalBank.backend.cucumber.steps.common.GraphQLSteps;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.customer.repository.RoleRepository;
import com.DigitalBank.backend.customer.entity.Role;

import java.time.LocalDate;

/**
 * Hooks de Cucumber para manejar la autenticación automáticamente.
 * Se encargan de establecer tokens JWT para escenarios que requieran autenticación.
 */
public class AuthenticationHooks {

    @Autowired
    private GraphQLSteps graphQLSteps;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Se ejecuta antes de escenarios que requieran autenticación de usuario.
     * Crea el usuario de prueba si no existe y establece un token JWT válido en los headers.
     */
    @Before("@requires_authentication")
    public void setupAuthentication() {
        // QA NOTE: Crear usuario de prueba si no existe antes de autenticar
        String email = "user@bank.com";
        String password = "SecurePass123!";

        if (!customerRepository.existsById("123456789")) {
            Customer customer = Customer.builder()
                .documentNumber("123456789")
                .documentType("CC")
                .name("Test User")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .status("ACTIVE")
                .birthDate(LocalDate.now().minusYears(25))
                .loginAttempts(0)
                .build();

            Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    role.setDescription("Usuario estándar");
                    return roleRepository.save(role);
                });

            customer.setRoles(java.util.Set.of(userRole));
            customerRepository.save(customer);
        }

        String token = graphQLSteps.buildAuthenticationToken(email, password);
        // QA NOTE: Si el token es null, no se establece autenticación
        if (token != null) {
            graphQLSteps.setAuthenticationHeader(token);
        } else {
            System.out.println("QA WARNING - No se pudo obtener token de autenticación");
        }
    }

    /**
     * Se ejecuta antes de escenarios que requieran autenticación de administrador.
     * Crea el administrador de prueba si no existe y establece un token JWT de administrador en los headers.
     */
    @Before("@requires_admin")
    public void setupAdminAuthentication() {
        // QA NOTE: Crear usuario admin si no existe
        String email = "admin@bank.com";
        String password = "AdminPass123!";
        String documentNumber = "admin123456";

        if (!customerRepository.existsById(documentNumber)) {
            Customer admin = Customer.builder()
                .documentNumber(documentNumber)
                .documentType("CC")
                .name("Admin User")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .status("ACTIVE")
                .birthDate(LocalDate.now().minusYears(30))
                .build();

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    role.setDescription("Administrador del sistema");
                    return roleRepository.save(role);
                });

            admin.setRoles(java.util.Set.of(adminRole));
            customerRepository.save(admin);
        }

        String token = graphQLSteps.buildAuthenticationToken(email, password);
        if (token != null) {
            graphQLSteps.setAuthenticationHeader(token);
        } else {
            System.out.println("QA WARNING - No se pudo obtener token de autenticación admin");
        }
    }

    /**
     * Se ejecuta después de escenarios con autenticación para limpiar los headers.
     */
    @After("@requires_authentication")
    public void clearAuthentication() {
        graphQLSteps.clearAuthentication();
    }
}
