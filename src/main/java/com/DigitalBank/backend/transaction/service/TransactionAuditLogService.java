package com.DigitalBank.backend.transaction.service;

import com.DigitalBank.backend.transaction.entity.TransactionAuditLog;
import com.DigitalBank.backend.transaction.repository.TransactionAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionAuditLogService {

    private final TransactionAuditLogRepository auditLogRepository;

    public TransactionAuditLogService(TransactionAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Registra un evento de auditoría para cualquier intento de transacción,
     * independientemente del resultado (COMPLETED, PENDING_VALIDATION, REJECTED).
     */
    @Transactional
    public void registrar(String transactionReference,
                          UUID sourceAccountId, UUID destinationAccountId,
                          String sourceCustomerDocument, String destinationCustomerDocument,
                          String operationType, BigDecimal amount,
                          BigDecimal balanceBeforeSource, BigDecimal balanceAfterSource,
                          BigDecimal balanceBeforeDestination, BigDecimal balanceAfterDestination,
                          String status) {
        TransactionAuditLog log = new TransactionAuditLog(
                transactionReference, sourceAccountId, destinationAccountId,
                sourceCustomerDocument, destinationCustomerDocument,
                operationType, amount,
                balanceBeforeSource, balanceAfterSource,
                balanceBeforeDestination, balanceAfterDestination,
                status
        );
        auditLogRepository.save(log);
    }

    /**
     * Consulta los registros de auditoría de una cuenta. Solo accesible por administradores.
     */
    @Transactional(readOnly = true)
    public List<TransactionAuditLog> consultarPorCuenta(String accountId) {
        return auditLogRepository.findByAccountId(UUID.fromString(accountId));
    }
}
