package com.DigitalBank.backend.cucumber.hooks;

import io.cucumber.java.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.DigitalBank.backend.account.repository.FinancialAccountRepository;
import com.DigitalBank.backend.customer.repository.CustomerRepository;
import com.DigitalBank.backend.transaction.repository.TransactionRepository;

/**
 * Hooks de Cucumber para manejar el ciclo de vida de la base de datos.
 * Se ejecutan antes y después de cada escenario para garantizar aislamiento.
 */
public class DatabaseHooks {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FinancialAccountRepository financialAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Se ejecuta antes de cada escenario para limpiar la base de datos.
     * Garantiza que cada escenario comience con estado limpio.
     */
    @Before(order = 1)
    @Transactional
    public void cleanDatabase() {
        transactionRepository.deleteAll();
        financialAccountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    /**
     * Se ejecuta después de cada escenario para limpieza adicional.
     */
    @After
    @Transactional
    public void cleanupScenario() {
        transactionRepository.deleteAll();
        financialAccountRepository.deleteAll();
        customerRepository.deleteAll();
    }
}
