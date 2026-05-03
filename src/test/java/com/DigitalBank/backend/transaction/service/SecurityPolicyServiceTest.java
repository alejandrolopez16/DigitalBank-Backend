package com.DigitalBank.backend.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.DigitalBank.backend.transaction.entity.SecurityPolicy;
import com.DigitalBank.backend.transaction.repository.SecurityPolicyRepository;

/**
 * Pruebas unitarias para el servicio SecurityPolicyService.
 */
@ExtendWith(MockitoExtension.class)
class SecurityPolicyServiceTest {

    @Mock
    private SecurityPolicyRepository securityPolicyRepository;

    @InjectMocks
    private SecurityPolicyService securityPolicyService;


    // ============= Pruebas HU 2.1.1: Configuración de validaciones de seguridad =============

    // Test: Validación de que se configure el límite de validación y se persista correctamente
    @Test
    void shouldConfigureValidationLimitAndPersist() {
        // Arrange
        SecurityPolicy currentPolicy = baseSecurityPolicy();
        BigDecimal newValidationLimit = new BigDecimal("250000.00");
        
        when(securityPolicyRepository.findById(1L))
                .thenReturn(Optional.of(currentPolicy));
        when(securityPolicyRepository.save(any(SecurityPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SecurityPolicy updated = securityPolicyService.updatePolicy(
                currentPolicy.getDailyLimit(),
                newValidationLimit,
                currentPolicy.getValidationType(),
                "admin"
        );

        // Assert
        assertEquals(newValidationLimit, updated.getValidationLimit());
        assertEquals("admin", updated.getUpdatedBy());
        verify(securityPolicyRepository).save(updated);
    }

    // Test: Validación de que se configure el tipo de validación y se persista correctamente
    @Test
    void shouldConfigureValidationTypeAndPersist() {
        // Arrange
        SecurityPolicy currentPolicy = baseSecurityPolicy();
        String newValidationType = "PASSWORD";
        
        when(securityPolicyRepository.findById(1L))
                .thenReturn(Optional.of(currentPolicy));
        when(securityPolicyRepository.save(any(SecurityPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SecurityPolicy updated = securityPolicyService.updatePolicy(
                currentPolicy.getDailyLimit(),
                currentPolicy.getValidationLimit(),
                newValidationType,
                "security_admin"
        );

        // Assert
        assertEquals("PASSWORD", updated.getValidationType());
        assertEquals("security_admin", updated.getUpdatedBy());
        verify(securityPolicyRepository).save(updated);
    }

    // Test: Validación de que se configure el límite diario y se persista correctamente
    @Test
    void shouldConfigureDailyLimitAndPersist() {
        // Arrange
        SecurityPolicy currentPolicy = baseSecurityPolicy();
        BigDecimal newDailyLimit = new BigDecimal("2000000.00");
        
        when(securityPolicyRepository.findById(1L))
                .thenReturn(Optional.of(currentPolicy));
        when(securityPolicyRepository.save(any(SecurityPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SecurityPolicy updated = securityPolicyService.updatePolicy(
                newDailyLimit,
                currentPolicy.getValidationLimit(),
                currentPolicy.getValidationType(),
                "admin"
        );

        // Assert
        assertEquals(newDailyLimit, updated.getDailyLimit());
        assertEquals("admin", updated.getUpdatedBy());
        verify(securityPolicyRepository).save(updated);
    }

    // Test: Validación de que se actualicen todos los parámetros de la política de seguridad a la vez y se persistan correctamente
    @Test
    void shouldUpdateAllSecurityPolicyParametersAtOnce() {
        // Arrange
        SecurityPolicy currentPolicy = baseSecurityPolicy();
        BigDecimal newDailyLimit = new BigDecimal("5000000.00");
        BigDecimal newValidationLimit = new BigDecimal("1000000.00");
        String newValidationType = "BIOMETRIC";
        
        when(securityPolicyRepository.findById(1L))
                .thenReturn(Optional.of(currentPolicy));
        when(securityPolicyRepository.save(any(SecurityPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SecurityPolicy updated = securityPolicyService.updatePolicy(
                newDailyLimit,
                newValidationLimit,
                newValidationType,
                "super_admin"
        );

        // Assert
        assertEquals(newDailyLimit, updated.getDailyLimit());
        assertEquals(newValidationLimit, updated.getValidationLimit());
        assertEquals(newValidationType, updated.getValidationType());
        assertEquals("super_admin", updated.getUpdatedBy());
        assertNotNull(updated.getUpdatedAt());
        verify(securityPolicyRepository).save(updated);
    }

    // Test: Validación de que se obtenga la política de seguridad existente correctamente
    @Test
    void shouldGetExistingSecurityPolicy() {
        // Arrange
        SecurityPolicy existingPolicy = baseSecurityPolicy();
        
        when(securityPolicyRepository.findById(1L))
                .thenReturn(Optional.of(existingPolicy));

        // Act
        SecurityPolicy retrieved = securityPolicyService.getPolicy();

        // Assert
        assertNotNull(retrieved);
        assertEquals(1L, retrieved.getIdSecurityPolicy());
        assertEquals(new BigDecimal("1000000.00"), retrieved.getDailyLimit());
        assertEquals(new BigDecimal("500000.00"), retrieved.getValidationLimit());
        assertEquals("OTP", retrieved.getValidationType());
        verify(securityPolicyRepository).findById(1L);
    }

    // Test: Validación de que se cree una política de seguridad por defecto si no existe y se persista correctamente
    @Test
    void shouldCreateDefaultPolicyWhenNotExists() {
        // Arrange
        when(securityPolicyRepository.findById(1L))
                .thenReturn(Optional.empty());
        when(securityPolicyRepository.save(any(SecurityPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SecurityPolicy policy = securityPolicyService.getPolicy();

        // Assert
        assertNotNull(policy);
        assertEquals(new BigDecimal("1000000"), policy.getDailyLimit());
        assertEquals(new BigDecimal("500000"), policy.getValidationLimit());
        assertEquals("OTP", policy.getValidationType());
        assertEquals("system", policy.getUpdatedBy());
        verify(securityPolicyRepository).save(any(SecurityPolicy.class));
    }

    // Test: Validación de que se mantenga la consistencia de los datos al actualizar la política de seguridad
    @Test
    void shouldMaintainConsistencyOnPolicyUpdate() {
        // Arrange
        SecurityPolicy initialPolicy = baseSecurityPolicy();
        BigDecimal newLimit = new BigDecimal("7500000.00");
        
        when(securityPolicyRepository.findById(1L))
                .thenReturn(Optional.of(initialPolicy));
        when(securityPolicyRepository.save(any(SecurityPolicy.class)))
                .thenAnswer(invocation -> {
                    SecurityPolicy policy = invocation.getArgument(0);
                    return SecurityPolicy.builder()
                            .idSecurityPolicy(policy.getIdSecurityPolicy())
                            .dailyLimit(policy.getDailyLimit())
                            .validationLimit(policy.getValidationLimit())
                            .validationType(policy.getValidationType())
                            .updatedAt(policy.getUpdatedAt())
                            .updatedBy(policy.getUpdatedBy())
                            .build();
                });

        // Act
        SecurityPolicy updated = securityPolicyService.updatePolicy(
                newLimit,
                initialPolicy.getValidationLimit(),
                initialPolicy.getValidationType(),
                "consistency_check"
        );

        // Assert
        assertEquals(newLimit, updated.getDailyLimit());
        assertEquals(initialPolicy.getValidationLimit(), updated.getValidationLimit());
        assertEquals("consistency_check", updated.getUpdatedBy());
        verify(securityPolicyRepository).save(any(SecurityPolicy.class));
    }

    // ============= Métodos axuiliares =============
    /**
     * Crea una política de seguridad base para las pruebas.
     */
    private SecurityPolicy baseSecurityPolicy() {
        return SecurityPolicy.builder()
                .idSecurityPolicy(1L)
                .dailyLimit(new BigDecimal("1000000.00"))
                .validationLimit(new BigDecimal("500000.00"))
                .validationType("OTP")
                .updatedAt(LocalDateTime.now())
                .updatedBy("system")
                .build();
    }
}
