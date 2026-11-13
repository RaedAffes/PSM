package com.psm.dto;

import com.psm.entity.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRequest(
    @NotNull Long userId,
    @NotNull ProjectRole projectRole
) {
}
