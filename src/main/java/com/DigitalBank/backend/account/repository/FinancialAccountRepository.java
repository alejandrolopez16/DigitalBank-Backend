package com.DigitalBank.backend.account.repository;

import com.DigitalBank.backend.account.entity.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, UUID> {
	List<FinancialAccount> findByCustomerDocumentNumber(String documentNumber);
	Optional<FinancialAccount> findByIdAndCustomerDocumentNumber(UUID id, String documentNumber);
}
