package com.DigitalBank.backend.transaction.controller;

import com.DigitalBank.backend.transaction.entity.TransactionAuditLog;
import com.DigitalBank.backend.transaction.service.TransactionAuditLogService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class TransactionAuditLogController {

    private final TransactionAuditLogService auditLogService;

    public TransactionAuditLogController(TransactionAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<TransactionAuditLog> consultarAuditoria(@Argument String accountId) {
        return auditLogService.consultarPorCuenta(accountId);
    }
}
