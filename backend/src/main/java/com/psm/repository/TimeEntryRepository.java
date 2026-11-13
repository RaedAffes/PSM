package com.psm.repository;

import com.psm.entity.TimeEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    @EntityGraph(attributePaths = { "user" })
    List<TimeEntry> findByTaskIdOrderByEntryDateDescIdDesc(Long taskId);

    @EntityGraph(attributePaths = { "user" })
    Optional<TimeEntry> findByIdAndTaskId(Long id, Long taskId);
}
