package com.DigitalBank.backend.transaction.controller;

import com.DigitalBank.backend.transaction.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.Map;

@Controller
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @MutationMapping(name = "ejecutarTransferencia")
    public Map<String, Object> ejecutarTransferencia(
            @Argument String sourceAccountId,
            @Argument String destinationAccountId,
            @Argument Float amount,
            @Argument String description) {

        BigDecimal monto = BigDecimal.valueOf(amount);
        return transactionService.ejecutarTransferencia(sourceAccountId, destinationAccountId, monto, description);
    }
}
