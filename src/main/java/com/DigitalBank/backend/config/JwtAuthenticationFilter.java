package com.DigitalBank.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
                                        
        String authHeader = request.getHeader("Authorization");

        // 1. Cláusula de Guardia: Si no hay token o no empieza con "Bearer ", lo dejamos pasar sin VIP
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraer el token limpio
        String token = authHeader.substring(7);
        
        try {
            // 3. Extraer datos usando los métodos exactos de tu JwtUtil
            String email = jwtUtil.extractUsername(token);
            String roles = jwtUtil.extractRoles(token);

            // 4. Validar el token y registrar al usuario en Spring Security
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null && jwtUtil.validateToken(token, email)) {
                
                 List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 5. Manejo de errores: Si el token expiró o está corrupto, lo avisamos en consola
            System.out.println("Token inválido o expirado: " + e.getMessage());
        }

        // 6. Continuar con el viaje de la petición hacia el controlador
        filterChain.doFilter(request, response);
    }
}
