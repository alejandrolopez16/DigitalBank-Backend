package com.DigitalBank.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email,String role){
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // 1. Extraer todas las "claims" (el contenido del token)
    private io.jsonwebtoken.Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Usa el SecretKey para verificar
                .build()
                .parseSignedClaims(token) // Lee el token firmado
                .getPayload(); // Extrae el contenido (email, roles, etc.)
    }

    // 2. Extraer el correo (Subject)
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // 3. Extraer el rol (Buscamos la palabra "role" exacta que usaste en la línea 26)
    public String extractRoles(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // 4. Validar que el token sea del usuario correcto y no haya expirado
    public boolean validateToken(String token, String email) {
        final String tokenEmail = extractUsername(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }

    // 5. Comprobar la fecha de expiración
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new java.util.Date());
    }
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        return !extractAllClaims(token).getExpiration().before(new Date());
    }
}
