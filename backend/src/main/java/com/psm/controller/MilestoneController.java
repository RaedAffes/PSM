package com.psm.controller;

import com.psm.dto.MilestoneRequest;
import com.psm.dto.MilestoneResponse;
import com.psm.entity.MilestoneStatus;
import com.psm.service.MilestoneService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/milestones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    @GetMapping
    public ResponseEntity<List<MilestoneResponse>> findAll(@RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(milestoneService.findAll(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MilestoneResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(milestoneService.findById(id));
    }

    @PostMapping
    public ResponseEntity<MilestoneResponse> create(@Valid @RequestBody MilestoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(milestoneService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MilestoneResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody MilestoneRequest request
    ) {
        return ResponseEntity.ok(milestoneService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MilestoneResponse> updateStatus(
        @PathVariable Long id,
        @RequestParam MilestoneStatus status
    ) {
        return ResponseEntity.ok(milestoneService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        milestoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
