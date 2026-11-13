package com.psm.repository;

import com.psm.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByLibelle(String libelle);
    boolean existsByLibelleIgnoreCase(String libelle);
    Optional<Role> findByLibelleIgnoreCase(String libelle);
    java.util.List<Role> findAllByOrderByLibelleAsc();
}
