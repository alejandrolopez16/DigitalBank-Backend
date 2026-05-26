package com.DigitalBank.backend.transaction.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_audit_logs")
public class TransactionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", length = 50, nullable = false, updatable = false)
    private String transactionReference;

    @Column(name = "source_account_id", updatable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", updatable = false)
    private UUID destinationAccountId;

    @Column(name = "source_customer_document", length = 20, updatable = false)
    private String sourceCustomerDocument;

    @Column(name = "destination_customer_document", length = 20, updatable = false)
    private String destinationCustomerDocument;

    @Column(name = "operation_type", length = 30, nullable = false, updatable = false)
    private String operationType;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "balance_before_source", precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceBeforeSource;

    @Column(name = "balance_after_source", precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceAfterSource;

    @Column(name = "balance_before_destination", precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceBeforeDestination;

    @Column(name = "balance_after_destination", precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceAfterDestination;

    @Column(name = "status", length = 20, nullable = false, updatable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TransactionAuditLog() {
        // Constructor protegido requerido por JPA
    }

    public TransactionAuditLog(String transactionReference,
                                UUID sourceAccountId, UUID destinationAccountId,
                                String sourceCustomerDocument, String destinationCustomerDocument,
                                String operationType, BigDecimal amount,
                                BigDecimal balanceBeforeSource, BigDecimal balanceAfterSource,
                                BigDecimal balanceBeforeDestination, BigDecimal balanceAfterDestination,
                                String status) {
        this.transactionReference = transactionReference;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.sourceCustomerDocument = sourceCustomerDocument;
        this.destinationCustomerDocument = destinationCustomerDocument;
        this.operationType = operationType;
        this.amount = amount;
        this.balanceBeforeSource = balanceBeforeSource;
        this.balanceAfterSource = balanceAfterSource;
        this.balanceBeforeDestination = balanceBeforeDestination;
        this.balanceAfterDestination = balanceAfterDestination;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // Solo getters — sin setters para garantizar inmutabilidad
    public Long getId() { return id; }
    public String getTransactionReference() { return transactionReference; }
    public String getSourceAccountId() { return sourceAccountId != null ? sourceAccountId.toString() : null; }
    public String getDestinationAccountId() { return destinationAccountId != null ? destinationAccountId.toString() : null; }
    public String getSourceCustomerDocument() { return sourceCustomerDocument; }
    public String getDestinationCustomerDocument() { return destinationCustomerDocument; }
    public String getOperationType() { return operationType; }
    public Double getAmount() { return amount != null ? amount.doubleValue() : null; }
    public Double getBalanceBeforeSource() { return balanceBeforeSource != null ? balanceBeforeSource.doubleValue() : null; }
    public Double getBalanceAfterSource() { return balanceAfterSource != null ? balanceAfterSource.doubleValue() : null; }
    public Double getBalanceBeforeDestination() { return balanceBeforeDestination != null ? balanceBeforeDestination.doubleValue() : null; }
    public Double getBalanceAfterDestination() { return balanceAfterDestination != null ? balanceAfterDestination.doubleValue() : null; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt != null ? createdAt.toString() : null; }
}
