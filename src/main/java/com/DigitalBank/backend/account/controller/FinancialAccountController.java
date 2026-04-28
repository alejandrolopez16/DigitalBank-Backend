package com.DigitalBank.backend.account.controller;

import com.DigitalBank.backend.account.service.FinancialAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.HashMap;

@Controller
public class FinancialAccountController {
    @Autowired
    private FinancialAccountService accountService;

    @QueryMapping(name = "validarCuentaDestino")
    public Map<String, Object> validarCuentaDestino(@Argument String accountId) {
        try {
            return accountService.preValidarCuentaDestino(accountId);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("isValid", false);
            errorResponse.put("message", "Error interno al validar la cuenta: " + e.getMessage());
            return errorResponse;
        }
    }
}
