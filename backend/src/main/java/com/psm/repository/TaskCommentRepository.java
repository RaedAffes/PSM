package com.psm.repository;

import com.psm.entity.TaskComment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = { "author" })
    List<TaskComment> findByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);

    @EntityGraph(attributePaths = { "author" })
    Optional<TaskComment> findByIdAndTaskId(Long id, Long taskId);
}
