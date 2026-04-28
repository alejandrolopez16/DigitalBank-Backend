package com.DigitalBank.backend.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "security_policies")
public class SecurityPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSecurityPolicy;

    @Column(name ="daily_limit",nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name="validation_limit",nullable = false, precision = 19, scale = 4)
    private BigDecimal validationLimit;

    @Column(name="validation_type",nullable = false, length = 50)
    private String validationType;

    @Column(name="updated_at",nullable = false)
    private LocalDateTime updatedAt;

    @Column(name="updated_by",nullable = false, length = 100)
    private String updatedBy;

    public String getUpdatedBy(){
        return updatedBy;       
    }
    public void setUpdatedBy(String updatedBy){
        this.updatedBy = updatedBy;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
