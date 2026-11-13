package com.psm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.psm.dto.AuthRequest;
import com.psm.dto.AuthResponse;
import com.psm.dto.RegisterRequest;
import com.psm.entity.Role;
import com.psm.repository.RoleRepository;
import com.psm.service.EmailService;
import com.psm.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByLibelle("ADMIN").isEmpty()) {
            roleRepository.save(new Role("ADMIN"));
        }
        if (roleRepository.findByLibelle("COLLABORATEUR").isEmpty()) {
            roleRepository.save(new Role("COLLABORATEUR"));
        }
    }

    @Test
    void registerAndLogin_shouldReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new.user@psm.com");
        request.setPassword("StrongPass123!");
        request.setTel("+21612345678");
        request.setNom("User");
        request.setPrenom("New");
        AuthResponse registerResponse = authService.register(request);
        assertThat(registerResponse.getToken()).isNotBlank();
        assertThat(registerResponse.getEmail()).isEqualTo("new.user@psm.com");
        assertThat(registerResponse.getRoles()).containsExactly("COLLABORATEUR");
        verify(emailService).sendRegistrationEmail(any());

        AuthResponse loginResponse = authService.login(new AuthRequest("new.user@psm.com", "StrongPass123!"));
        assertThat(loginResponse.getToken()).isNotBlank();
        assertThat(loginResponse.getRoles()).containsExactly("COLLABORATEUR");
    }
}
