package com.psm.dto;

import com.psm.entity.Project;
import java.math.BigDecimal;

public record ProjectResponse(
    Long id,
    String libelle,
    BigDecimal budget,
    Integer duree,
    String description,
    Long clientId,
    ClientSummary client,
    String imageUrl
) {

    public static ProjectResponse from(Project project) {
        var client = project.getClient();
        return new ProjectResponse(
            project.getId(),
            project.getLibelle(),
            project.getBudget(),
            project.getDuree(),
            project.getDescription(),
            client.getId(),
            new ClientSummary(client.getId(), client.getNom()),
            project.getImageStorageKey() == null ? null : "/api/projects/" + project.getId() + "/image"
        );
    }

    public record ClientSummary(Long id, String nom) {
    }
}
