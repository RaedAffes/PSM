package com.psm.dto;

import com.psm.entity.TaskPriority;
import com.psm.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDate startDate,
    LocalDate dueDate,
    @NotNull Long projectId,
    Long assignedUserId
) {
}
