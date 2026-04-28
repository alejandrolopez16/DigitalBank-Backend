package com.DigitalBank.backend.transaction.repository;

import com.DigitalBank.backend.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.sourceAccount = :accountId AND t.createdAt >= :startOfDay AND t.status IN ('COMPLETED', 'PENDING_VALIDATION')")
    BigDecimal sumarTransaccionesDelDia(@Param("accountId") UUID accountId, @Param("startOfDay") LocalDateTime startOfDay);
}
