package com.psm.repository;

import com.psm.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @EntityGraph(attributePaths = "client")
    @Override
    List<Project> findAll();

    @EntityGraph(attributePaths = "client")
    @Override
    Optional<Project> findById(Long id);

    List<Project> findByClientId(Long clientId);
}
