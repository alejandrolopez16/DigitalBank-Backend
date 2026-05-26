package com.DigitalBank.backend.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.DigitalBank.backend.transaction.service.TransactionAuditLogService;

import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;

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

    @Autowired
    private TransactionAuditLogService auditLogService;

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

        // Capturar saldos antes de cualquier modificación para el log de auditoría
        BigDecimal balanceBeforeSource = cuentaOrigen.getBalance();
        BigDecimal balanceBeforeDestination = cuentaDestino.getBalance();
        String sourceDoc = cuentaOrigen.getCustomer() != null ? cuentaOrigen.getCustomer().getDocumentNumber() : null;
        String destDoc = cuentaDestino.getCustomer() != null ? cuentaDestino.getCustomer().getDocumentNumber() : null;
        String auditRef = UUID.randomUUID().toString();

        if ("BLOCKED".equalsIgnoreCase(cuentaOrigen.getStatus())
                || "BLOCKED".equalsIgnoreCase(cuentaOrigen.getStatus())) {
            auditLogService.registrar(auditRef, sourceUuid, destinationUuid, sourceDoc, destDoc,
                    "TRANSFER", amount, balanceBeforeSource, balanceBeforeSource,
                    balanceBeforeDestination, balanceBeforeDestination, "REJECTED");
            return crearRespuestaError(response, "Operación rechazada: La cuenta de origen está bloqueada.");
        }

        if ("BLOCKED".equalsIgnoreCase(cuentaDestino.getStatus())
                || "BLOCKED".equalsIgnoreCase(cuentaDestino.getStatus())) {
            auditLogService.registrar(auditRef, sourceUuid, destinationUuid, sourceDoc, destDoc,
                    "TRANSFER", amount, balanceBeforeSource, balanceBeforeSource,
                    balanceBeforeDestination, balanceBeforeDestination, "REJECTED");
            return crearRespuestaError(response, "Operación rechazada: La cuenta de destino está bloqueada.");
        }

        if (cuentaOrigen.getBalance().compareTo(amount) < 0) {
            auditLogService.registrar(auditRef, sourceUuid, destinationUuid, sourceDoc, destDoc,
                    "TRANSFER", amount, balanceBeforeSource, balanceBeforeSource,
                    balanceBeforeDestination, balanceBeforeDestination, "REJECTED");
            return crearRespuestaError(response, "Fondos insuficientes en la cuenta origen");
        }

        SecurityPolicy policy = securityPolicyService.getPolicy();
        LocalDateTime inicioDelDia = LocalDate.now().atStartOfDay();
        BigDecimal gastadoHoy = transactionRepository.sumarTransaccionesDelDia(sourceUuid, inicioDelDia);

        if (gastadoHoy.add(amount).compareTo(policy.getDailyLimit()) > 0) {
            auditLogService.registrar(auditRef, sourceUuid, destinationUuid, sourceDoc, destDoc,
                    "TRANSFER", amount, balanceBeforeSource, balanceBeforeSource,
                    balanceBeforeDestination, balanceBeforeDestination, "REJECTED");
            return crearRespuestaError(response, "Rechazo: Se ha superado el límite diario de transferencias.");
        }

        // --- EJECUCIÓN O RETENCIÓN ---
        Transaction transaccion = new Transaction();
        transaccion.setReference(auditRef);
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

            auditLogService.registrar(auditRef, sourceUuid, destinationUuid, sourceDoc, destDoc,
                    "TRANSFER", amount, balanceBeforeSource, balanceBeforeSource,
                    balanceBeforeDestination, balanceBeforeDestination, "PENDING_VALIDATION");

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

        //Notificación a cuenta Origen
        Map<String, Object> operacionOrigen = new HashMap<>();
        operacionOrigen.put("fecha", transaccion.getCreatedAt().toString());
        operacionOrigen.put("tipo", "DEBITO");
        operacionOrigen.put("monto", amount);
        operacionOrigen.put("description", concepto);
        operacionOrigen.put("targetAccountId", cuentaOrigen.getId().toString());
        operacionOrigen.put("estado",transaccion.getStatus());
        transactionSink.tryEmitNext(operacionOrigen);

        //Notificación a cuenta Destino
        Map<String, Object> operacionDestino = new HashMap<>();
        operacionDestino.put("fecha", transaccion.getCreatedAt().toString());
        operacionDestino.put("tipo", "CREDITO");
        operacionDestino.put("monto", amount);
        operacionDestino.put("description", concepto);
        operacionDestino.put("targetAccountId", cuentaDestino.getId().toString());
        operacionDestino.put("estado",transaccion.getStatus());
        transactionSink.tryEmitNext(operacionDestino);

        transaccion.setStatus("COMPLETED");
        transactionRepository.save(transaccion);

        auditLogService.registrar(auditRef, sourceUuid, destinationUuid, sourceDoc, destDoc,
                "TRANSFER", amount, balanceBeforeSource, cuentaOrigen.getBalance(),
                balanceBeforeDestination, cuentaDestino.getBalance(), "COMPLETED");

        response.put("success", true);
        response.put("message", "Transferencia Exitosa");
        response.put("transactionReference", transaccion.getReference());
        response.put("status", "COMPLETED");
        response.put("destinationName", nombreDestino);
        return response;

    }

    private void emitirEventoAWebsocket(String fecha, String tipo, BigDecimal monto, String estado, String targetId) {
        Map<String, Object> evento = new HashMap<>();
        evento.put("fecha", fecha);
        evento.put("tipo", tipo);
        evento.put("monto", monto);
        evento.put("estado", estado);
        evento.put("targetAccountId", targetId);
        transactionSink.tryEmitNext(evento);
    }

    private Map<String, Object> crearRespuestaError(Map<String, Object> response, String mensaje) {
        response.put("success", false);
        response.put("message", mensaje);
        response.put("transactionReference", null);
        response.put("status", "REJECTED");
        response.put("destinationName", null);
        return response;
    }



    private final Sinks.Many<Map<String, Object>> transactionSink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<Map<String, Object>> streamTransactions(String accountId) {
        return transactionSink.asFlux()
        .filter(operacion->accountId.equals(operacion.get("targetAccountId").toString()));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> consultarHistorial(String accountIdStr) {
        UUID accountId = UUID.fromString(accountIdStr);

        Optional<FinancialAccount> accountOpt = financialAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) throw new IllegalArgumentException("La cuenta no existe.");

        FinancialAccount cuenta = accountOpt.get();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!cuenta.getCustomer().getEmail().equalsIgnoreCase(auth.getName())) {
            throw new IllegalArgumentException("Acceso denegado: No tienes permisos para ver este historial.");
        }

        List<Transaction> transacciones = transactionRepository.consultarHistorialPorCuenta(accountId);
        List<Map<String, Object>> historial = new ArrayList<>();

        for (Transaction t : transacciones) {
            Map<String, Object> operacion = new HashMap<>();
            operacion.put("fecha", t.getCreatedAt().toString());
            operacion.put("monto", t.getAmount());
            operacion.put("estado", t.getStatus());
            operacion.put("tipo", t.getSourceAccount().equals(accountId) ? "EGRESO" : "INGRESO");
            historial.add(operacion);
        }
        return historial;
    }

}
