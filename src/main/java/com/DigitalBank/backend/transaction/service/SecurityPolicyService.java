package com.DigitalBank.backend.transaction.service;

import com.DigitalBank.backend.transaction.entity.SecurityPolicy;
import com.DigitalBank.backend.transaction.repository.SecurityPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class SecurityPolicyService {
    @Autowired
    private SecurityPolicyRepository repository;

    public SecurityPolicy getPolicy(){
        return repository.findById(1L).orElseGet(()->{
            SecurityPolicy defaultPolicy= new SecurityPolicy();
            defaultPolicy.setDailyLimit(new BigDecimal(1000000));
            defaultPolicy.setValidationLimit(new BigDecimal(500000));
            defaultPolicy.setValidationType("OTP");
            defaultPolicy.setUpdatedBy("system");
            return repository.save(defaultPolicy);
        });
    }

    public SecurityPolicy updatePolicy(BigDecimal dailyLimit, BigDecimal threshold, String validationType, String updatedBy){
        SecurityPolicy policy = getPolicy(); // Busca la del ID 1
        
        policy.setDailyLimit(dailyLimit);
        policy.setValidationLimit(threshold);
        policy.setValidationType(validationType);
        policy.setUpdatedBy(updatedBy);
        
        return repository.save(policy);
    }
}
