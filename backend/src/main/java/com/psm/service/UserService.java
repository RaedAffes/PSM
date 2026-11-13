package com.psm.service;

import com.psm.dto.UserCreateRequest;
import com.psm.dto.UserResponse;
import com.psm.dto.UserUpdateRequest;
import com.psm.entity.Role;
import com.psm.entity.User;
import com.psm.repository.RoleRepository;
import com.psm.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTel(request.getTel().trim());
        user.setNom(request.getNom().trim());
        user.setPrenom(request.getPrenom().trim());
        user.setRoles(resolveRoles(request.getRoles()));
        User createdUser = userRepository.save(user);
        emailService.sendAdminUserCreatedEmail(createdUser);
        return toResponse(createdUser);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new RuntimeException("Email already exists");
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 6) {
                throw new IllegalArgumentException("Password must contain at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setEmail(email);
        user.setTel(request.getTel().trim());
        user.setNom(request.getNom().trim());
        user.setPrenom(request.getPrenom().trim());
        user.setRoles(resolveRoles(request.getRoles()));
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, String authenticatedEmail) {
        User user = findUser(id);
        if (user.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new IllegalArgumentException("Cannot delete own account");
        }
        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
        return roleNames.stream()
            .map(name -> {
                String normalizedName = name.trim().toUpperCase(Locale.ROOT);
                if (!normalizedName.equals("ADMIN") && !normalizedName.equals("COLLABORATEUR")) {
                    throw new IllegalArgumentException("Only ADMIN and COLLABORATEUR roles are supported");
                }
                return roleRepository.findByLibelleIgnoreCase(normalizedName)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            })
            .collect(Collectors.toSet());
    }

    private UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
            .map(Role::getLibelle)
            .collect(Collectors.toSet());
        return new UserResponse(user.getId(), user.getEmail(), user.getTel(), user.getNom(), user.getPrenom(), roles);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
