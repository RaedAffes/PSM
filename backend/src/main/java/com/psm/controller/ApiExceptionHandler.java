package com.psm.controller;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " : " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, message.isBlank() ? "Les données envoyées sont invalides." : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableRequest() {
        return response(HttpStatus.BAD_REQUEST, "Le format des données envoyées est invalide.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataConflict() {
        return response(HttpStatus.CONFLICT, "L'opération est impossible : ces données existent déjà ou sont encore utilisées.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize() {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "Le fichier dépasse la taille maximale autorisée.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return response(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue.");
        }

        return switch (message) {
            case "Email already exists" -> response(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
            case "Role already exists" -> response(HttpStatus.CONFLICT, "Ce rôle existe déjà.");
            case "Role is assigned to users" -> response(HttpStatus.CONFLICT, "Ce rôle est encore attribué à des utilisateurs.");
            case "User not found" -> response(HttpStatus.NOT_FOUND, "Utilisateur introuvable.");
            case "Role not found" -> response(HttpStatus.NOT_FOUND, "Rôle introuvable.");
            case "Cannot delete own account" -> response(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas supprimer votre propre compte.");
            case "Built-in roles cannot be renamed" -> response(HttpStatus.BAD_REQUEST, "Les rôles système ne peuvent pas être renommés.");
            case "Built-in roles cannot be deleted" -> response(HttpStatus.BAD_REQUEST, "Les rôles système ne peuvent pas être supprimés.");
            case "At least one role is required" -> response(HttpStatus.BAD_REQUEST, "Au moins un rôle doit être sélectionné.");
            case "Password must contain at least 6 characters" -> response(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 6 caractères.");
            case "Only ADMIN and COLLABORATEUR roles are supported" -> response(HttpStatus.BAD_REQUEST, "Seuls les rôles ADMIN et COLLABORATEUR sont autorisés.");
            case "Client not found" -> response(HttpStatus.NOT_FOUND, "Client introuvable.");
            case "Project not found" -> response(HttpStatus.NOT_FOUND, "Projet introuvable.");
            case "Project image not found" -> response(HttpStatus.NOT_FOUND, "Aucune image pour ce projet.");
            case "Project does not belong to client" -> response(HttpStatus.BAD_REQUEST, "Le projet sélectionné n'appartient pas à ce client.");
            case "Invoice not found" -> response(HttpStatus.NOT_FOUND, "Facture introuvable.");
            case "Invoice file not found" -> response(HttpStatus.NOT_FOUND, "Fichier de facture introuvable.");
            case "Project member not found" -> response(HttpStatus.NOT_FOUND, "Membre du projet introuvable.");
            case "User is already a project member" -> response(HttpStatus.CONFLICT, "Cet utilisateur fait déjà partie du projet.");
            case "Task not found" -> response(HttpStatus.NOT_FOUND, "Tâche introuvable.");
            case "Task status is required" -> response(HttpStatus.BAD_REQUEST, "Le statut de la tâche est obligatoire.");
            case "Milestone not found" -> response(HttpStatus.NOT_FOUND, "Jalon introuvable.");
            case "Milestone status is required" -> response(HttpStatus.BAD_REQUEST, "Le statut du jalon est obligatoire.");
            case "Comment not found" -> response(HttpStatus.NOT_FOUND, "Commentaire introuvable.");
            case "You cannot delete this comment" -> response(HttpStatus.FORBIDDEN, "Vous ne pouvez pas supprimer ce commentaire.");
            case "Time entry not found" -> response(HttpStatus.NOT_FOUND, "Entrée de temps introuvable.");
            case "You cannot delete this time entry" -> response(HttpStatus.FORBIDDEN, "Vous ne pouvez pas supprimer cette entrée de temps.");
            case "Invoice number already exists" -> response(HttpStatus.CONFLICT, "Ce numéro de facture est déjà utilisé.");
            case "Stored file is missing" -> response(HttpStatus.NOT_FOUND, "Le fichier n'est plus disponible sur le serveur.");
            case "File is empty" -> response(HttpStatus.BAD_REQUEST, "Le fichier envoyé est vide.");
            case "File too large" -> response(HttpStatus.PAYLOAD_TOO_LARGE, "Le fichier dépasse la taille maximale autorisée.");
            case "Unsupported file type" -> response(HttpStatus.BAD_REQUEST, "Ce type de fichier n'est pas autorisé.");
            case "Invalid file name" -> response(HttpStatus.BAD_REQUEST, "Le nom du fichier est invalide.");
            case "Collaborator role not found" -> response(HttpStatus.INTERNAL_SERVER_ERROR, "Le rôle Collaborateur n'est pas configuré.");
            case "Authentication failed" -> response(HttpStatus.UNAUTHORIZED, "E-mail ou mot de passe incorrect.");
            default -> response(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue lors de l'opération.");
        };
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
