package com.DigitalBank.backend.transaction.controller;

import com.DigitalBank.backend.transaction.entity.FinancialReport;
import com.DigitalBank.backend.transaction.service.FinancialReportService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class FinancialReportController {
    private final FinancialReportService financialReportService;

    public FinancialReportController(FinancialReportService financialReportService) {
        this.financialReportService = financialReportService;
    }

    @QueryMapping
    public FinancialReport reporteActividadFinanciera(@Argument String startDate, @Argument String endDate) {
        // La seguridad y extracción del usuario ya la manejamos dentro del Service
        return financialReportService.generateReport(startDate, endDate);
    }
}
