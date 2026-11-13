package com.psm.config;

import com.psm.entity.Role;
import com.psm.entity.User;
import com.psm.repository.RoleRepository;
import com.psm.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@psm.com";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${psm.admin.email:admin@psm.com}")
    private String adminEmail;

    @Value("${psm.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = roleRepository.findByLibelle("ADMIN").orElseGet(() -> roleRepository.save(new Role("ADMIN")));
        Role collaboratorRole = roleRepository.findByLibelle("COLLABORATEUR").orElseGet(() -> roleRepository.save(new Role("COLLABORATEUR")));
        List<Role> customRoles = roleRepository.findAll().stream()
            .filter(role -> !isBuiltInRole(role.getLibelle()))
            .toList();
        if (!customRoles.isEmpty()) {
            for (User user : userRepository.findAll()) {
                if (user.getRoles().removeIf(role -> !isBuiltInRole(role.getLibelle()))) {
                    userRepository.save(user);
                }
            }
            userRepository.flush();
            roleRepository.deleteAll(customRoles);
            roleRepository.flush();
            log.info("Removed {} custom role(s) and their user assignments", customRoles.size());
        }

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        if (admin == null && !DEFAULT_ADMIN_EMAIL.equals(adminEmail)) {
            admin = userRepository.findByEmail(DEFAULT_ADMIN_EMAIL).orElse(null);
        }

        if (admin == null) {
            if (adminPassword.isBlank()) {
                log.warn("Skipping admin account creation because psm.admin.password is not configured");
                return;
            }
            admin = new User();
            admin.setNom("Admin");
            admin.setPrenom("PSM");
            admin.setTel("+21600000000");
        }

        admin.setEmail(adminEmail);
        if (!adminPassword.isBlank()) {
            admin.setPassword(passwordEncoder.encode(adminPassword));
        }
        Set<Role> roles = admin.getRoles() == null ? new HashSet<>() : new HashSet<>(admin.getRoles());
        roles.add(adminRole);
        roles.add(collaboratorRole);
        admin.setRoles(roles);
        userRepository.save(admin);
    }

    private boolean isBuiltInRole(String name) {
        return "ADMIN".equalsIgnoreCase(name) || "COLLABORATEUR".equalsIgnoreCase(name);
    }
}
