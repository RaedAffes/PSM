package com.psm.repository;

import com.psm.entity.Milestone;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    @EntityGraph(attributePaths = { "project" })
    List<Milestone> findByProjectIdOrderByDueDateAscIdAsc(Long projectId);

    @EntityGraph(attributePaths = { "project" })
    @Override
    List<Milestone> findAll();

    @EntityGraph(attributePaths = { "project" })
    Optional<Milestone> findByIdAndProjectId(Long id, Long projectId);
}
