package com.psm.dto;

import com.psm.entity.ProjectMember;
import com.psm.entity.ProjectRole;
import java.time.Instant;

public record ProjectMemberResponse(
    Long id,
    Long projectId,
    Long userId,
    String userName,
    String userEmail,
    ProjectRole projectRole,
    Instant joinedAt
) {

    public static ProjectMemberResponse from(ProjectMember member) {
        var user = member.getUser();
        return new ProjectMemberResponse(
            member.getId(),
            member.getProject().getId(),
            user.getId(),
            (user.getNom() + " " + user.getPrenom()).trim(),
            user.getEmail(),
            member.getProjectRole(),
            member.getJoinedAt()
        );
    }
}
