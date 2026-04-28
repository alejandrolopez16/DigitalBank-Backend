package com.DigitalBank.backend.transaction.controller;

import com.DigitalBank.backend.transaction.entity.SecurityPolicy;
import com.DigitalBank.backend.transaction.service.SecurityPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.util.Map;

@Controller
public class SecurityPolicyController {
    @Autowired
    private SecurityPolicyService service;

    @QueryMapping
    public SecurityPolicy politicasSeguridad() {
        return service.getPolicy();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    
    public SecurityPolicy ActualizarPoliticaSeguridad(@Argument Map<String, Object> input){
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        String username=auth.getName();
        BigDecimal dailyLimit = new BigDecimal(input.get("dailyLimit").toString());
        BigDecimal validationLimit = new BigDecimal(input.get("validationLimit").toString());
        String validationType = input.get("validationType").toString();
        return service.updatePolicy(dailyLimit, validationLimit, validationType, username);
    }
}
