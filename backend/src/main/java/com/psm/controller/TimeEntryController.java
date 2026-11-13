package com.psm.controller;

import com.psm.dto.TimeEntryRequest;
import com.psm.dto.TimeEntryResponse;
import com.psm.service.TimeEntryService;
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
@RequestMapping("/api/tasks/{taskId}/time-entries")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @GetMapping
    public ResponseEntity<List<TimeEntryResponse>> findAll(@PathVariable Long taskId) {
        return ResponseEntity.ok(timeEntryService.findAll(taskId));
    }

    @PostMapping
    public ResponseEntity<TimeEntryResponse> create(
        @PathVariable Long taskId,
        @Valid @RequestBody TimeEntryRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(timeEntryService.create(taskId, request, authentication.getName()));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long taskId,
        @PathVariable Long entryId,
        Authentication authentication
    ) {
        timeEntryService.delete(taskId, entryId, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ADMIN".equals(authority.getAuthority()));
    }
}
