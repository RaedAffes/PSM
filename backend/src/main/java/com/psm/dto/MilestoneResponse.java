package com.psm.dto;

import com.psm.entity.Milestone;
import com.psm.entity.MilestoneStatus;
import java.time.Instant;
import java.time.LocalDate;

public record MilestoneResponse(
    Long id,
    String title,
    String description,
    LocalDate dueDate,
    MilestoneStatus status,
    Long projectId,
    String projectLibelle,
    boolean overdue,
    Instant createdAt
) {

    public static MilestoneResponse from(Milestone milestone) {
        var project = milestone.getProject();
        boolean overdue = milestone.getStatus() != MilestoneStatus.COMPLETED
            && milestone.getDueDate() != null
            && milestone.getDueDate().isBefore(LocalDate.now());

        return new MilestoneResponse(
            milestone.getId(),
            milestone.getTitle(),
            milestone.getDescription(),
            milestone.getDueDate(),
            milestone.getStatus(),
            project == null ? null : project.getId(),
            project == null ? null : project.getLibelle(),
            overdue,
            milestone.getCreatedAt()
        );
    }
}
