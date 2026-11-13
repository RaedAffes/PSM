package com.psm.dto;

import com.psm.entity.MilestoneStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MilestoneRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 1000) String description,
    LocalDate dueDate,
    MilestoneStatus status,
    @NotNull Long projectId
) {
}
