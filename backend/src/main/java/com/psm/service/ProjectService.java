package com.psm.service;

import com.psm.entity.Client;
import com.psm.entity.Project;
import com.psm.dto.ProjectResponse;
import com.psm.repository.ClientRepository;
import com.psm.repository.InvoiceRepository;
import com.psm.repository.ProjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        return projectRepository.findAll().stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        return ProjectResponse.from(findProject(id));
    }

    @Transactional
    public ProjectResponse save(Project project, Long clientId) {
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new RuntimeException("Client not found"));
        project.setClient(client);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, Project projectData, Long clientId) {
        Project project = findProject(id);
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new RuntimeException("Client not found"));

        project.setLibelle(projectData.getLibelle());
        project.setBudget(projectData.getBudget());
        project.setDuree(projectData.getDuree());
        project.setDescription(projectData.getDescription());
        project.setClient(client);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateImage(Long id, MultipartFile file) {
        Project project = findProject(id);
        String previousKey = project.getImageStorageKey();
        FileStorageService.StoredFile stored = fileStorageService.storeImage(file);

        project.setImageStorageKey(stored.storageKey());
        project.setImageContentType(stored.contentType());
        project.setImageFileName(stored.originalName());

        try {
            Project saved = projectRepository.save(project);
            if (previousKey != null) {
                fileStorageService.delete(previousKey);
            }
            return ProjectResponse.from(saved);
        } catch (RuntimeException exception) {
            fileStorageService.delete(stored.storageKey());
            throw exception;
        }
    }

    @Transactional
    public ProjectResponse deleteImage(Long id) {
        Project project = findProject(id);
        String previousKey = project.getImageStorageKey();
        project.setImageStorageKey(null);
        project.setImageContentType(null);
        project.setImageFileName(null);
        Project saved = projectRepository.save(project);
        if (previousKey != null) {
            fileStorageService.delete(previousKey);
        }
        return ProjectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ImageFile loadImage(Long id) {
        Project project = findProject(id);
        if (project.getImageStorageKey() == null) {
            throw new RuntimeException("Project image not found");
        }
        String contentType = project.getImageContentType() == null
            ? "application/octet-stream"
            : project.getImageContentType();
        return new ImageFile(project.getImageFileName(), contentType, fileStorageService.load(project.getImageStorageKey()));
    }

    @Transactional
    public void delete(Long id) {
        Project project = findProject(id);
        String imageKey = project.getImageStorageKey();
        invoiceRepository.unlinkProject(id);
        projectRepository.delete(project);
        if (imageKey != null) {
            fileStorageService.delete(imageKey);
        }
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public record ImageFile(String fileName, String contentType, Resource resource) {
    }
}
