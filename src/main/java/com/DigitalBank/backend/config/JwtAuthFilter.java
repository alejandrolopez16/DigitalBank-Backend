package com.DigitalBank.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter{
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtutil){
        this.jwtUtil = jwtutil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterchain) throws ServletException,IOException{
        String authHeader=request.getHeader("Authorization");
        String token=null;
        String email=null;

        if(authHeader!=null && authHeader.startsWith("Bearer ")){
            token=authHeader.substring(7);
            try{
                email=jwtUtil.extractUsername(token);
            }catch(Exception e){
                System.out.println("Error extrayendo el token "+ e.getMessage());
            }
        }
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(token,email)){
                String roles=jwtUtil.extractRoles(token);
                List<GrantedAuthority> authorities=Arrays.stream(roles.split(",")).map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authToken=new UsernamePasswordAuthenticationToken(email,null,authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterchain.doFilter(request, response);
    }
}
