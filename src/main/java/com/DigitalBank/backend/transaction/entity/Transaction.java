package com.DigitalBank.backend.transaction.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @Column(name = "reference", length = 50, nullable = false)
    private String reference;

    @Column(name = "source_account")
    private UUID sourceAccount;

    @Column(name = "destination_account")
    private UUID destinationAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
