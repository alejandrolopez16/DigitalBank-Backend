package com.DigitalBank.backend.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.account.service.FinancialAccountService;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.transaction.entity.SecurityPolicy;
import com.DigitalBank.backend.transaction.entity.Transaction;
import com.DigitalBank.backend.transaction.repository.TransactionRepository;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FinancialAccountRepository financialAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SecurityPolicyService securityPolicyService;

    @Transactional
    public Map<String, Object> ejecutarTransferencia(String sourceId, String destinationId, BigDecimal amount,
            String description) {
        Map<String, Object> response = new HashMap<>();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return crearRespuestaError(response, "El monto de la transferencia debe ser mayor a cero");
        }

        String concepto = (description != null) ? description.trim() : "Transferencia";
        if (concepto.length() > 50) {
            return crearRespuestaError(response, "La descripción no puede exceder los 50 caracteres");
        }

        UUID sourceUuid = UUID.fromString(sourceId);
        UUID destinationUuid = UUID.fromString(destinationId);

        Optional<FinancialAccount> sourceOptional = financialAccountRepository.findById(sourceUuid);
        Optional<FinancialAccount> destinationOptional = financialAccountRepository.findById(destinationUuid);

        if (sourceOptional.isEmpty()) {
            return crearRespuestaError(response, "Cuenta origen no encontrada");
        }
        if (destinationOptional.isEmpty()) {
            return crearRespuestaError(response, "Cuenta destino no encontrada");
        }

        FinancialAccount cuentaOrigen = sourceOptional.get();
        FinancialAccount cuentaDestino = destinationOptional.get();

        if ("BLOCKED".equalsIgnoreCase(cuentaOrigen.getStatus())
                || "BLOCKED".equalsIgnoreCase(cuentaOrigen.getStatus())) {
            return crearRespuestaError(response, "Operación rechazada: La cuenta de origen está bloqueada.");
        }

        if ("BLOCKED".equalsIgnoreCase(cuentaDestino.getStatus())
                || "BLOCKED".equalsIgnoreCase(cuentaDestino.getStatus())) {
            return crearRespuestaError(response, "Operación rechazada: La cuenta de destino está bloqueada.");
        }

        if (cuentaOrigen.getBalance().compareTo(amount) < 0) {
            return crearRespuestaError(response, "Fondos insuficientes en la cuenta origen");
        }

        SecurityPolicy policy = securityPolicyService.getPolicy();
        LocalDateTime inicioDelDia = LocalDate.now().atStartOfDay();
        BigDecimal gastadoHoy = transactionRepository.sumarTransaccionesDelDia(sourceUuid, inicioDelDia);

        if (gastadoHoy.add(amount).compareTo(policy.getDailyLimit()) > 0) {
            return crearRespuestaError(response, "Rechazo: Se ha superado el límite diario de transferencias.");
        }

        // --- EJECUCIÓN O RETENCIÓN ---
        Transaction transaccion = new Transaction();
        transaccion.setReference(UUID.randomUUID().toString());
        transaccion.setSourceAccount(sourceUuid);
        transaccion.setDestinationAccount(destinationUuid);
        transaccion.setAmount(amount);
        transaccion.setType("TRANSFER");
        transaccion.setDescription(concepto);
        transaccion.setCreatedAt(LocalDateTime.now());

        String nombreDestino = "";
        if (cuentaDestino.getCustomer() != null) {
            // Buscamos el cliente por su ID (documentNumber) para asegurar que cargue los
            // datos
            Optional<Customer> clienteOpt = customerRepository
                    .findById(cuentaDestino.getCustomer().getDocumentNumber());

            if (clienteOpt.isPresent()) {
                // 2. Si existe, usamos el método estático que ya definimos para ofuscar
                nombreDestino = FinancialAccountService.ofuscarNombre(clienteOpt.get().getName());
            }
        }
        // 6. Validar Umbral Extra (¿Pide OTP?)
        if (amount.compareTo(policy.getValidationLimit()) > 0) {
            transaccion.setStatus("PENDING_VALIDATION");
            transactionRepository.save(transaccion);

            response.put("success", true);
            response.put("message", "La transacción requiere validación extra (" + policy.getValidationType() + ").");
            response.put("transactionReference", transaccion.getReference());
            response.put("status", "PENDING_VALIDATION");
            response.put("destinationName", nombreDestino);
            return response;
        }

        // 7. Si todo está perfecto y no pide OTP, movemos el dinero (Ejecución Exitosa)
        cuentaOrigen.setBalance(cuentaOrigen.getBalance().subtract(amount));
        cuentaDestino.setBalance(cuentaDestino.getBalance().add(amount));

        financialAccountRepository.save(cuentaOrigen);
        financialAccountRepository.save(cuentaDestino);

        transaccion.setStatus("COMPLETED");
        transactionRepository.save(transaccion);

        response.put("success", true);
        response.put("message", "Transferencia Exitosa");
        response.put("transactionReference", transaccion.getReference());
        response.put("status", "COMPLETED");
        response.put("destinationName", nombreDestino);
        return response;

    }

    private Map<String, Object> crearRespuestaError(Map<String, Object> response, String mensaje) {
        response.put("success", false);
        response.put("message", mensaje);
        response.put("transactionReference", null);
        response.put("status", "REJECTED");
        return response;
    }
}
