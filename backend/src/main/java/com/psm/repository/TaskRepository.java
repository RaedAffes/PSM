package com.psm.repository;

import com.psm.entity.Task;
import com.psm.entity.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = { "project", "assignee" })
    @Override
    List<Task> findAll();

    @EntityGraph(attributePaths = { "project", "assignee" })
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = { "project", "assignee" })
    List<Task> findByProjectIdOrderByDueDateAscIdAsc(Long projectId);

    @EntityGraph(attributePaths = { "project", "assignee" })
    List<Task> findByStatusOrderByDueDateAscIdAsc(TaskStatus status);

    @EntityGraph(attributePaths = { "project", "assignee" })
    List<Task> findByProjectIdAndStatusOrderByDueDateAscIdAsc(Long projectId, TaskStatus status);

    @EntityGraph(attributePaths = { "project", "assignee" })
    List<Task> findByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);
}
