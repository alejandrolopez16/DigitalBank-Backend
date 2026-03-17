package com.DigitalBank.backend.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "customers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder

public class Customer {
    @Id
    @Column(name="document_number",length=20,nullable=false,unique=true)
    private String documentNumber;

    @Column(name="document_type",length=20,nullable=false)
    private String documentType;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(unique = true, nullable = false, length = 50)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "document_front", length = 255)
    private String documentFrontUrl;

    @Column(name = "document_back", length = 255)
    private String documentBackUrl;

    @Column(name = "selfie_url", length = 255)
    private String selfieUrl;

    @Column(name = "rejection_comments", columnDefinition = "TEXT")
    private String rejectionComments;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "customer_roles",
            joinColumns = @JoinColumn(name = "customer_document"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}