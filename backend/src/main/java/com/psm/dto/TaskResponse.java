package com.psm.dto;

import com.psm.entity.Task;
import com.psm.entity.TaskPriority;
import com.psm.entity.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDate startDate,
    LocalDate dueDate,
    Long projectId,
    String projectLibelle,
    Long assignedUserId,
    String assignedUserName,
    boolean overdue,
    Instant createdAt,
    Instant updatedAt
) {

    public static TaskResponse from(Task task) {
        var project = task.getProject();
        var assignee = task.getAssignee();
        boolean overdue = task.getStatus() != TaskStatus.DONE
            && task.getDueDate() != null
            && task.getDueDate().isBefore(LocalDate.now());

        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getStartDate(),
            task.getDueDate(),
            project == null ? null : project.getId(),
            project == null ? null : project.getLibelle(),
            assignee == null ? null : assignee.getId(),
            assignee == null ? null : (assignee.getNom() + " " + assignee.getPrenom()).trim(),
            overdue,
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
