package com.psm.service;

import com.psm.dto.ProjectMemberRequest;
import com.psm.dto.ProjectMemberResponse;
import com.psm.entity.Project;
import com.psm.entity.ProjectMember;
import com.psm.entity.User;
import com.psm.repository.ProjectMemberRepository;
import com.psm.repository.ProjectRepository;
import com.psm.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> findAll(Long projectId) {
        requireProject(projectId);
        return projectMemberRepository.findByProjectIdOrderByJoinedAtAscIdAsc(projectId).stream()
            .map(ProjectMemberResponse::from)
            .toList();
    }

    @Transactional
    public ProjectMemberResponse add(Long projectId, ProjectMemberRequest request) {
        Project project = requireProject(projectId);
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new RuntimeException("User is already a project member");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setProjectRole(request.projectRole());
        return ProjectMemberResponse.from(projectMemberRepository.save(member));
    }

    @Transactional
    public ProjectMemberResponse updateRole(Long projectId, Long memberId, ProjectMemberRequest request) {
        ProjectMember member = findMember(projectId, memberId);
        member.setProjectRole(request.projectRole());
        return ProjectMemberResponse.from(projectMemberRepository.save(member));
    }

    @Transactional
    public void remove(Long projectId, Long memberId) {
        projectMemberRepository.delete(findMember(projectId, memberId));
    }

    private ProjectMember findMember(Long projectId, Long memberId) {
        return projectMemberRepository.findByIdAndProjectId(memberId, projectId)
            .orElseThrow(() -> new RuntimeException("Project member not found"));
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
    }
}
