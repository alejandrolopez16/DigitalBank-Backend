package com.DigitalBank.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
<<<<<<< HEAD
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
=======
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
>>>>>>> 0fe90907a27b7025670cda6d4ffddf1e85a7a613
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
<<<<<<< HEAD
@EnableWebSecurity
=======
>>>>>>> 0fe90907a27b7025670cda6d4ffddf1e85a7a613
@EnableMethodSecurity

public class SecurityConfig {

<<<<<<< HEAD
    private final JwtAuthFilter jwtAuthFilter;
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }
=======
    private final GraphQlPublicOperationFilter graphQlPublicOperationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(GraphQlPublicOperationFilter graphQlPublicOperationFilter,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.graphQlPublicOperationFilter = graphQlPublicOperationFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    
>>>>>>> 0fe90907a27b7025670cda6d4ffddf1e85a7a613
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityfilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests((auth) -> auth
<<<<<<< HEAD
                .requestMatchers("/graphql", "/graphiql", "/api/media/**").permitAll() 
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter,UsernamePasswordAuthenticationFilter.class);
=======
                .anyRequest().permitAll()
            )
            .addFilterBefore(graphQlPublicOperationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
>>>>>>> 0fe90907a27b7025670cda6d4ffddf1e85a7a613
        return http.build();
    }
}
