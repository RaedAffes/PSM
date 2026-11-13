package com.psm.controller;

import com.psm.entity.Project;
import com.psm.dto.ProjectResponse;
import com.psm.service.ProjectService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> findAll() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> create(@RequestParam Long clientId, @RequestBody Project project) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.save(project, clientId));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> update(@PathVariable Long id, @RequestParam Long clientId, @RequestBody Project project) {
        return ResponseEntity.ok(projectService.update(id, project, clientId));
    }

    @PostMapping(value = "/projects/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectResponse> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(projectService.updateImage(id, file));
    }

    @DeleteMapping("/projects/{id}/image")
    public ResponseEntity<ProjectResponse> deleteImage(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.deleteImage(id));
    }

    @GetMapping("/projects/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id) {
        ProjectService.ImageFile image = projectService.loadImage(id);
        ContentDisposition disposition = ContentDisposition
            .builder("inline")
            .filename(image.fileName() == null ? "project-image" : image.fileName(), StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(image.resource());
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
