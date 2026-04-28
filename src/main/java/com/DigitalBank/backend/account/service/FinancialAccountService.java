package com.DigitalBank.backend.account.service;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.customer.entity.Customer;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org. springframework.beans.factory.annotation.Autowired;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
public class FinancialAccountService {

    private final FinancialAccountRepository financialAccountRepository;
    private final CustomerRepository customerRepository;

    public FinancialAccountService(FinancialAccountRepository financialAccountRepository,
                                   CustomerRepository customerRepository) {
        this.financialAccountRepository = financialAccountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public FinancialAccount createFinancialAccount(String documentNumber) {
        Customer authenticatedCustomer = getAuthenticatedCustomer();

        if (!authenticatedCustomer.getDocumentNumber().equals(documentNumber)) {
            throw new IllegalArgumentException("Solo puedes crear una cuenta financiera para tu propio usuario");
        }

        Customer customer = customerRepository.findById(documentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        FinancialAccount account = FinancialAccount.builder()
                .customer(customer)
                .balance(BigDecimal.ZERO)
                .build();

        return financialAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<FinancialAccount> getMyFinancialAccounts() {
        Customer authenticatedCustomer = getAuthenticatedCustomer();
        return financialAccountRepository.findByCustomerDocumentNumber(authenticatedCustomer.getDocumentNumber());
    }

    @Transactional(readOnly = true)
    public FinancialAccount getMyFinancialAccountById(UUID accountId) {
        Customer authenticatedCustomer = getAuthenticatedCustomer();
        return financialAccountRepository.findByIdAndCustomerDocumentNumber(accountId, authenticatedCustomer.getDocumentNumber())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta financiera no encontrada para el usuario autenticado"));
    }

    private Customer getAuthenticatedCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("Debe iniciar sesión con un token JWT válido para consultar o crear cuentas financieras");
        }

        String authenticatedEmail = authentication.getName();
        return customerRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    public FinancialAccount blockAccount(UUID accountId, String email) {

        Customer customer = getAuthenticatedCustomer();

        FinancialAccount account = financialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        // Validar que sea el dueño
        if (!account.getCustomer().getEmail().equals(customer.getEmail())) {
            throw new RuntimeException("No autorizado");
        }

        account.setStatus("BLOCKED");
        return financialAccountRepository.save(account);
    }

    public FinancialAccount unblockAccount(UUID accountId, String email) {
        Customer customer = getAuthenticatedCustomer();

        FinancialAccount account = financialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        if (!account.getCustomer().getEmail().equals(customer.getEmail())) {
            throw new RuntimeException("No autorizado");
        }

        account.setStatus("ACTIVE");
        return financialAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Map<String,Object> preValidarCuentaDestino(String accountIdString){
        Map<String,Object> response = new HashMap<>();

        try{
            UUID accountId = UUID.fromString(accountIdString);
            Optional<FinancialAccount> accountOpt = financialAccountRepository.findById(accountId);
            if(accountOpt.isEmpty()){
                response.put("isValid", false);
                response.put("message", "Cuenta destino no encontrada");
                return response;
            }
            FinancialAccount account = accountOpt.get();

            if("BLOCKED".equals(account.getStatus())){
                response.put("isValid", false);
                response.put("message", "Cuenta destino bloqueada");
                response.put("accountStatus", account.getStatus());
                return response;
            }

            Customer cliente = account.getCustomer();

            if (cliente == null) {
                response.put("isValid", false);
                response.put("message", "Cliente de la cuenta destino no encontrado");
                return response;
            }
            String nombreOfuscado=ofuscarNombre(cliente.getName());

            response.put("isValid", true);
            response.put("message", "Cuenta destino válida");
            response.put("obfuscatedName", nombreOfuscado);
            response.put("accountStatus", account.getStatus());
            return response;
        }catch (IllegalArgumentException e) {
            // Si el ID no es un UUID válido, devolvemos el mapa con el error en lugar de null
            response.put("isValid", false);
            response.put("message", "Formato de número de cuenta inválido.");
            return response;
        }
    }

    public static String ofuscarNombre(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        
        String[] words = fullName.trim().split("\\s+");
        StringBuilder obfuscated = new StringBuilder();

        for (String word : words) {
            if (word.length() <= 2) {
                obfuscated.append(word).append(" ");
            } else {
                char first = word.charAt(0);
                char last = word.charAt(word.length() - 1);
                String middle = "*".repeat(word.length() - 2);
                obfuscated.append(first).append(middle).append(last).append(" ");
            }
        }
        return obfuscated.toString().trim();
    }

}
