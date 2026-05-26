package com.DigitalBank.backend.transaction.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public class FinancialReport {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;
    private String periodStart;
    private String periodEnd;
    private String message;

    public FinancialReport(String periodStart, String periodEnd, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netProfit, String message) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netProfit = netProfit;
        this.message = message;
    }

    public String getPeriodStart() { return periodStart; }

    public String getPeriodEnd() { return periodEnd; }

    public Double getTotalIncome() {
        return totalIncome != null ? totalIncome.doubleValue() : 0.0;
    }

    public Double getTotalExpense() {
        return totalExpense != null ? totalExpense.doubleValue() : 0.0;
    }

    public Double getNetProfit() {
        return netProfit != null ? netProfit.doubleValue() : 0.0;
    }

    public String getMessage() { return message; }
}
