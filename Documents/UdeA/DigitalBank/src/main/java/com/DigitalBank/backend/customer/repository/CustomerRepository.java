package com.DigitalBank.backend.customer.repository;

import com.DigitalBank.backend.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Boolean existsByEmail(String email); //Valida si el email ya existe en la base de datos
    Boolean existsByDocumentNumber(String documentNumber); //Valida si el número de documento ya existe en la base de datos
    List<Customer> findByStatus(String status); //Busca clientes por su estado (PENDING, APPROVED, REJECTED)
    Optional<Customer> findByEmail(String email); //Busca un cliente por su email, útil para autenticación
}