package com.psm.controller;

import com.psm.dto.TaskCommentRequest;
import com.psm.dto.TaskCommentResponse;
import com.psm.service.TaskCommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    @GetMapping
    public ResponseEntity<List<TaskCommentResponse>> findAll(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskCommentService.findAll(taskId));
    }

    @PostMapping
    public ResponseEntity<TaskCommentResponse> create(
        @PathVariable Long taskId,
        @Valid @RequestBody TaskCommentRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskCommentService.create(taskId, request, authentication.getName()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long taskId,
        @PathVariable Long commentId,
        Authentication authentication
    ) {
        taskCommentService.delete(taskId, commentId, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ADMIN".equals(authority.getAuthority()));
    }
}
