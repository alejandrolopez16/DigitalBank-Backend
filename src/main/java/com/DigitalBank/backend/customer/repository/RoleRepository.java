package com.DigitalBank.backend.customer.repository;

import com.DigitalBank.backend.customer.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name); //Busca un rol por su nombre, útil para asignar roles a los clientes
}
