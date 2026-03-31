package com.DigitalBank.backend.customer.entity;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter

public class AuthResponse {
    private String token;
    private String status;
    private String role;
    private String message;
}
