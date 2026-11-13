package com.psm.service;

import com.psm.dto.MilestoneRequest;
import com.psm.dto.MilestoneResponse;
import com.psm.entity.Milestone;
import com.psm.entity.MilestoneStatus;
import com.psm.entity.Project;
import com.psm.repository.MilestoneRepository;
import com.psm.repository.ProjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<MilestoneResponse> findAll(Long projectId) {
        List<Milestone> milestones = projectId == null
            ? milestoneRepository.findAll()
            : milestoneRepository.findByProjectIdOrderByDueDateAscIdAsc(projectId);
        return milestones.stream().map(MilestoneResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MilestoneResponse findById(Long id) {
        return MilestoneResponse.from(findMilestone(id));
    }

    @Transactional
    public MilestoneResponse create(MilestoneRequest request) {
        Milestone milestone = new Milestone();
        apply(milestone, request, true);
        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    @Transactional
    public MilestoneResponse update(Long id, MilestoneRequest request) {
        Milestone milestone = findMilestone(id);
        apply(milestone, request, false);
        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    @Transactional
    public MilestoneResponse updateStatus(Long id, MilestoneStatus status) {
        Milestone milestone = findMilestone(id);
        if (status == null) {
            throw new RuntimeException("Milestone status is required");
        }
        milestone.setStatus(status);
        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    @Transactional
    public void delete(Long id) {
        milestoneRepository.delete(findMilestone(id));
    }

    private void apply(Milestone milestone, MilestoneRequest request, boolean creating) {
        Project project = projectRepository.findById(request.projectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

        milestone.setTitle(request.title().trim());
        milestone.setDescription(blankToNull(request.description()));
        milestone.setDueDate(request.dueDate());
        milestone.setProject(project);
        milestone.setStatus(request.status() == null
            ? (creating ? MilestoneStatus.PLANNED : milestone.getStatus())
            : request.status());
    }

    private Milestone findMilestone(Long id) {
        return milestoneRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
