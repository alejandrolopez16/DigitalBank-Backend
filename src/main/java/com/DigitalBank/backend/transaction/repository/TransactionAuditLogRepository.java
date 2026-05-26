package com.DigitalBank.backend.transaction.repository;

import com.DigitalBank.backend.transaction.entity.TransactionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionAuditLogRepository extends JpaRepository<TransactionAuditLog, Long> {

    @Query("SELECT a FROM TransactionAuditLog a " +
           "WHERE a.sourceAccountId = :accountId OR a.destinationAccountId = :accountId " +
           "ORDER BY a.createdAt DESC")
    List<TransactionAuditLog> findByAccountId(@Param("accountId") UUID accountId);
}
