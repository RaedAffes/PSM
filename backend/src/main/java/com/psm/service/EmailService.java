package com.psm.service;

import com.psm.entity.User;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class EmailService {

    private static final RestClient RESEND_CLIENT = RestClient.create("https://api.resend.com");

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:}")
    private String fromEmail;

    @Value("${psm.admin.email:admin@psm.com}")
    private String adminEmail;

    public void sendRegistrationEmail(User user) {
        if (apiKey.isBlank() || fromEmail.isBlank()) {
            log.warn("Registration email for {} was not sent: configure RESEND_API_KEY and RESEND_FROM_EMAIL", user.getEmail());
            return;
        }

        Map<String, String> email = Map.of(
            "from", fromEmail,
            "to", adminEmail,
            "subject", "Nouvelle inscription PSM",
            "text", "Une nouvelle inscription a été enregistrée.\nE-mail : " + user.getEmail()
                + "\nNom : " + user.getNom() + " " + user.getPrenom()
        );

        RESEND_CLIENT.post()
            .uri("/emails")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(email)
            .retrieve()
            .toBodilessEntity();

        log.info("Registration notification sent to admin {}", adminEmail);
    }

    public void sendAdminUserCreatedEmail(User user) {
        if (apiKey.isBlank() || fromEmail.isBlank()) {
            log.warn("Admin user-created email for {} was not sent: configure RESEND_API_KEY and RESEND_FROM_EMAIL", user.getEmail());
            return;
        }

        Map<String, String> email = Map.of(
            "from", fromEmail,
            "to", adminEmail,
            "subject", "Nouvel utilisateur créé dans PSM",
            "text", "Un nouvel utilisateur a été créé par l'Admin.\n"
                + "Nom : " + user.getPrenom() + " " + user.getNom()
                + "\nE-mail : " + user.getEmail()
        );

        RESEND_CLIENT.post()
            .uri("/emails")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(email)
            .retrieve()
            .toBodilessEntity();

        log.info("Admin user-created notification sent to {}", adminEmail);
    }
}
