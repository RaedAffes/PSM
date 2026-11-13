package com.psm.controller;

import com.psm.dto.ProjectMemberRequest;
import com.psm.dto.ProjectMemberResponse;
import com.psm.service.ProjectMemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @GetMapping
    public ResponseEntity<List<ProjectMemberResponse>> findAll(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectMemberService.findAll(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectMemberResponse> add(
        @PathVariable Long projectId,
        @Valid @RequestBody ProjectMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberService.add(projectId, request));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<ProjectMemberResponse> updateRole(
        @PathVariable Long projectId,
        @PathVariable Long memberId,
        @Valid @RequestBody ProjectMemberRequest request
    ) {
        return ResponseEntity.ok(projectMemberService.updateRole(projectId, memberId, request));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> remove(@PathVariable Long projectId, @PathVariable Long memberId) {
        projectMemberService.remove(projectId, memberId);
        return ResponseEntity.noContent().build();
    }
}
