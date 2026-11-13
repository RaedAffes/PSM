package com.psm.service;

import com.psm.dto.AuthRequest;
import com.psm.dto.AuthResponse;
import com.psm.dto.ProfileUpdateRequest;
import com.psm.dto.RegisterRequest;
import com.psm.dto.UserResponse;
import com.psm.entity.Role;
import com.psm.entity.User;
import com.psm.repository.RoleRepository;
import com.psm.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setTel(request.getTel());

        Role collaboratorRole = roleRepository.findByLibelle("COLLABORATEUR")
            .orElseThrow(() -> new RuntimeException("Collaborator role not found"));
        user.setRoles(new HashSet<>(Set.of(collaboratorRole)));
        userRepository.save(user);
        emailService.sendRegistrationEmail(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRoles().stream().map(Role::getLibelle).collect(Collectors.toList()));
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        if (!authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication failed");
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRoles().stream().map(Role::getLibelle).collect(Collectors.toList()));
    }

    @Transactional
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 6) {
                throw new IllegalArgumentException("Password must contain at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setTel(request.getTel().trim());
        user.setNom(request.getNom().trim());
        user.setPrenom(request.getPrenom().trim());

        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getTel(),
            user.getNom(),
            user.getPrenom(),
            user.getRoles().stream().map(Role::getLibelle).collect(Collectors.toSet())
        );
    }
}
