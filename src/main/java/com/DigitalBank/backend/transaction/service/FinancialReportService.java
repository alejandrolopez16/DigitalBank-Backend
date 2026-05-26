package com.DigitalBank.backend.transaction.service;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.transaction.entity.FinancialReport;
import com.DigitalBank.backend.transaction.entity.Transaction;
import com.DigitalBank.backend.transaction.repository.TransactionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinancialReportService {
private final TransactionRepository transactionRepository;
    private final FinancialAccountRepository financialAccountRepository;
    private final CustomerRepository customerRepository;

    public FinancialReportService(TransactionRepository transactionRepository, FinancialAccountRepository financialAccountRepository, CustomerRepository customerRepository) {
        this.transactionRepository = transactionRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public FinancialReport generateReport(String startDateStr, String endDateStr) {
        // 1. Identificar al usuario autenticado (Criterio 4)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Obtener TODAS las cuentas financieras de este usuario
        List<FinancialAccount> userAccounts = financialAccountRepository.findByCustomerDocumentNumber(customer.getDocumentNumber());
        
        if (userAccounts.isEmpty()) {
            return new FinancialReport(startDateStr, endDateStr, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "El usuario no posee cuentas activas.");
        }

        // Extraer solo los UUIDs de las cuentas del usuario
        List<UUID> userAccountIds = userAccounts.stream()
                .map(FinancialAccount::getId)
                .collect(Collectors.toList());

        // 3. Configurar Fechas (Criterio 1)
        LocalDateTime startDate = LocalDate.parse(startDateStr).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(endDateStr).atTime(LocalTime.MAX);

        // 4. Buscar transacciones en el periodo
        List<Transaction> transactions = transactionRepository.consultarTransaccionesPorCuentasYFechas(userAccountIds, startDate, endDate);

        // 5. Validar si hay operaciones (Criterio 3)
        if (transactions.isEmpty()) {
            return new FinancialReport(startDateStr, endDateStr, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "No existen transacciones para el periodo definido.");
        }

        // 6. Calcular Ingresos y Egresos (Criterio 2)
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            // Opcional: Solo sumar transacciones completadas
            if (!"COMPLETED".equals(t.getStatus())) continue;

            boolean isSourceMine = userAccountIds.contains(t.getSourceAccount());
            boolean isDestinationMine = userAccountIds.contains(t.getDestinationAccount());

            if (isSourceMine && isDestinationMine) {
                // Transferencia entre cuentas del mismo dueño (Suma en ambas columnas para cuadre contable)
                totalExpense = totalExpense.add(t.getAmount());
                totalIncome = totalIncome.add(t.getAmount());
            } else if (isSourceMine) {
                // Salió de una de mis cuentas
                totalExpense = totalExpense.add(t.getAmount());
            } else if (isDestinationMine) {
                // Entró a una de mis cuentas
                totalIncome = totalIncome.add(t.getAmount());
            }
        }

        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        return new FinancialReport(startDateStr, endDateStr, totalIncome, totalExpense, netProfit, "Reporte generado exitosamente.");
    }
}
