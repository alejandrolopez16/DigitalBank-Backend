package com.DigitalBank.backend.transaction.repository;

import com.DigitalBank.backend.transaction.entity.SecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, Long> {
    
}
