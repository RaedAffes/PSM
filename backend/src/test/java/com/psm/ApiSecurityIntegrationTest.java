package com.psm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.psm.dto.AuthResponse;
import com.psm.dto.UserResponse;
import com.psm.entity.Client;
import com.psm.entity.Role;
import com.psm.entity.User;
import com.psm.config.DataSeeder;
import com.psm.repository.ClientRepository;
import com.psm.repository.ProjectRepository;
import com.psm.repository.RoleRepository;
import com.psm.repository.UserRepository;
import com.psm.service.EmailService;
import com.psm.service.JwtService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class ApiSecurityIntegrationTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DataSeeder dataSeeder;

    @MockitoBean
    private EmailService emailService;

    private String adminToken;
    private String collaboratorToken;
    private Long adminId;
    private Long adminRoleId;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = roleRepository.save(new Role("ADMIN"));
        Role collaboratorRole = roleRepository.save(new Role("COLLABORATEUR"));
        adminRoleId = adminRole.getId();
        User admin = saveUser("admin.test@psm.local", adminRole);
        User collaborator = saveUser("collaborator.test@psm.local", collaboratorRole);
        adminId = admin.getId();
        adminToken = jwtService.generateToken(admin);
        collaboratorToken = jwtService.generateToken(collaborator);
    }

    @Test
    void publicRegistrationCannotAssignAdminAndLoginReturnsCollaboratorRole() {
        Map<String, Object> registration = Map.of(
            "email", "new.user@psm.local",
            "password", "StrongPass123!",
            "tel", "+21612345678",
            "nom", "User",
            "prenom", "New",
            "roles", List.of("ADMIN")
        );

        ResponseEntity<AuthResponse> registered = exchange(
            "/api/auth/register", HttpMethod.POST, registration, null, AuthResponse.class
        );

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registered.getBody().getRoles()).containsExactly("COLLABORATEUR");

        ResponseEntity<AuthResponse> login = exchange(
            "/api/auth/login",
            HttpMethod.POST,
            Map.of("email", "new.user@psm.local", "password", "StrongPass123!"),
            null,
            AuthResponse.class
        );
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().getRoles()).containsExactly("COLLABORATEUR");
    }

    @Test
    void collaboratorCanReadAndCreateButCannotUpdateOrDeleteClientsAndProjects() {
        ResponseEntity<String> clients = exchange(
            "/api/clients", HttpMethod.GET, null, collaboratorToken, String.class
        );
        assertThat(clients.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Client> createdClient = exchange(
            "/api/clients",
            HttpMethod.POST,
            Map.of("nom", "Client Test", "email", "client@psm.local", "tel", "+21600000000", "adresse", "Tunis"),
            collaboratorToken,
            Client.class
        );
        assertThat(createdClient.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long clientId = createdClient.getBody().getId();

        ResponseEntity<String> clientUpdate = exchange(
            "/api/clients/" + clientId,
            HttpMethod.PUT,
            Map.of("nom", "Updated", "email", "client@psm.local", "tel", "+21600000000", "adresse", "Tunis"),
            collaboratorToken,
            String.class
        );
        assertThat(clientUpdate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> projectCreate = exchange(
            "/api/projects?clientId=" + clientId,
            HttpMethod.POST,
            Map.of("libelle", "Project Test", "budget", 1000, "duree", 5, "description", "Test"),
            collaboratorToken,
            String.class
        );
        assertThat(projectCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> clientsWithProjects = exchange(
            "/api/clients", HttpMethod.GET, null, collaboratorToken, String.class
        );
        assertThat(clientsWithProjects.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(clientsWithProjects.getBody())
            .contains("\"nom\":\"Client Test\"")
            .doesNotContain("\"projects\"");

        Long projectId = projectRepository.findAll().get(0).getId();
        ResponseEntity<String> collaboratorClientDelete = exchange(
            "/api/clients/" + clientId, HttpMethod.DELETE, null, collaboratorToken, String.class
        );
        assertThat(collaboratorClientDelete.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> projectUpdate = exchange(
            "/api/projects/" + projectId + "?clientId=" + clientId,
            HttpMethod.PUT,
            Map.of("libelle", "Updated", "budget", 1000, "duree", 5, "description", "Test"),
            collaboratorToken,
            String.class
        );
        assertThat(projectUpdate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> collaboratorProjects = exchange(
            "/api/projects", HttpMethod.GET, null, collaboratorToken, String.class
        );
        assertThat(collaboratorProjects.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(collaboratorProjects.getBody()).contains("\"client\":{\"id\":");
        assertThat(collaboratorProjects.getBody()).contains("\"nom\":\"Client Test\"");

        ResponseEntity<String> projectDelete = exchange(
            "/api/projects/" + projectId, HttpMethod.DELETE, null, collaboratorToken, String.class
        );
        assertThat(projectDelete.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> adminProjectUpdate = exchange(
            "/api/projects/" + projectId + "?clientId=" + clientId,
            HttpMethod.PUT,
            Map.of("libelle", "Admin Updated", "budget", 1500, "duree", 6, "description", "Updated"),
            adminToken,
            String.class
        );
        assertThat(adminProjectUpdate.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> adminProjectDelete = exchange(
            "/api/projects/" + projectId, HttpMethod.DELETE, null, adminToken, String.class
        );
        assertThat(adminProjectDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> adminClients = exchange(
            "/api/clients", HttpMethod.GET, null, adminToken, String.class
        );
        assertThat(adminClients.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> adminProjects = exchange(
            "/api/projects", HttpMethod.GET, null, adminToken, String.class
        );
        assertThat(adminProjects.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Client> adminCreatedClient = exchange(
            "/api/clients",
            HttpMethod.POST,
            Map.of("nom", "Admin Client", "email", "admin.client@psm.local", "tel", "+21600000001", "adresse", "Tunis"),
            adminToken,
            Client.class
        );
        assertThat(adminCreatedClient.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<String> adminProjectCreate = exchange(
            "/api/projects?clientId=" + adminCreatedClient.getBody().getId(),
            HttpMethod.POST,
            Map.of("libelle", "Admin Project", "budget", 500, "duree", 3, "description", "Admin test"),
            adminToken,
            String.class
        );
        assertThat(adminProjectCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> adminClientUpdate = exchange(
            "/api/clients/" + clientId,
            HttpMethod.PUT,
            Map.of("nom", "Admin Updated", "email", "client@psm.local", "tel", "+21600000000", "adresse", "Tunis"),
            adminToken,
            String.class
        );
        assertThat(adminClientUpdate.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> adminClientDelete = exchange(
            "/api/clients/" + clientId, HttpMethod.DELETE, null, adminToken, String.class
        );
        assertThat(adminClientDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void adminCanManageUsersAndRolesButCannotDeleteOwnAccount() {
        ResponseEntity<String> collaboratorUsers = exchange(
            "/api/users", HttpMethod.GET, null, collaboratorToken, String.class
        );
        assertThat(collaboratorUsers.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> collaboratorRoles = exchange(
            "/api/roles", HttpMethod.GET, null, collaboratorToken, String.class
        );
        assertThat(collaboratorRoles.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> collaboratorUserCreate = exchange(
            "/api/users",
            HttpMethod.POST,
            Map.of(
                "email", "forbidden.user@psm.local",
                "password", "StrongPass123!",
                "tel", "+21612345678",
                "nom", "Forbidden",
                "prenom", "User",
                "roles", List.of("COLLABORATEUR")
            ),
            collaboratorToken,
            String.class
        );
        assertThat(collaboratorUserCreate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> collaboratorRoleCreate = exchange(
            "/api/roles", HttpMethod.POST, Map.of("libelle", "FORBIDDEN"), collaboratorToken, String.class
        );
        assertThat(collaboratorRoleCreate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> adminUsers = exchange(
            "/api/users", HttpMethod.GET, null, adminToken, String.class
        );
        assertThat(adminUsers.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminUsers.getBody()).doesNotContain("\"password\"");

        ResponseEntity<UserResponse> createdUser = exchange(
            "/api/users",
            HttpMethod.POST,
            Map.of(
                "email", "managed.user@psm.local",
                "password", "StrongPass123!",
                "tel", "+21612345678",
                "nom", "Managed",
                "prenom", "User",
                "roles", List.of("COLLABORATEUR")
            ),
            adminToken,
            UserResponse.class
        );
        assertThat(createdUser.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createdUser.getBody().roles()).containsExactly("COLLABORATEUR");
        verify(emailService).sendAdminUserCreatedEmail(any(User.class));

        UserResponse user = createdUser.getBody();
        ResponseEntity<UserResponse> updatedUser = exchange(
            "/api/users/" + user.id(),
            HttpMethod.PUT,
            Map.of(
                "email", "managed.user@psm.local",
                "password", "",
                "tel", "+21687654321",
                "nom", "Updated",
                "prenom", "User",
                "roles", List.of("ADMIN")
            ),
            adminToken,
            UserResponse.class
        );
        assertThat(updatedUser.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedUser.getBody().roles()).containsExactly("ADMIN");

        ResponseEntity<String> selfDelete = exchange(
            "/api/users/" + adminId, HttpMethod.DELETE, null, adminToken, String.class
        );
        assertThat(selfDelete.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<String> userDelete = exchange(
            "/api/users/" + user.id(), HttpMethod.DELETE, null, adminToken, String.class
        );
        assertThat(userDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> createdRole = exchange(
            "/api/roles", HttpMethod.POST, Map.of("libelle", "AUDITOR"), adminToken, String.class
        );
        assertThat(createdRole.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<String> customRoleAssignment = exchange(
            "/api/users",
            HttpMethod.POST,
            Map.of(
                "email", "custom.role@psm.local",
                "password", "StrongPass123!",
                "tel", "+21612345678",
                "nom", "Custom",
                "prenom", "Role",
                "roles", List.of("AUDITOR")
            ),
            adminToken,
            String.class
        );
        assertThat(customRoleAssignment.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<String> builtInRoleRename = exchange(
            "/api/roles/" + adminRoleId,
            HttpMethod.PUT,
            Map.of("libelle", "ROOT"),
            adminToken,
            String.class
        );
        assertThat(builtInRoleRename.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<String> builtInRoleDelete = exchange(
            "/api/roles/" + adminRoleId,
            HttpMethod.DELETE,
            null,
            adminToken,
            String.class
        );
        assertThat(builtInRoleDelete.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void authenticatedUserCanUpdateOwnProfileWithoutChangingRoles() {
        ResponseEntity<String> anonymousUpdate = exchange(
            "/api/auth/me",
            HttpMethod.PUT,
            Map.of("nom", "Updated", "prenom", "Profile", "tel", "+21612345678"),
            null,
            String.class
        );
        assertThat(anonymousUpdate.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<UserResponse> updatedProfile = exchange(
            "/api/auth/me",
            HttpMethod.PUT,
            Map.of(
                "nom", "Updated",
                "prenom", "Profile",
                "tel", "+21612345678",
                "password", "",
                "roles", List.of("ADMIN")
            ),
            collaboratorToken,
            UserResponse.class
        );

        assertThat(updatedProfile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedProfile.getBody().nom()).isEqualTo("Updated");
        assertThat(updatedProfile.getBody().prenom()).isEqualTo("Profile");
        assertThat(updatedProfile.getBody().tel()).isEqualTo("+21612345678");
        assertThat(updatedProfile.getBody().roles()).containsExactly("COLLABORATEUR");
        assertThat(userRepository.findByEmail("collaborator.test@psm.local").orElseThrow().getRoles())
            .extracting(Role::getLibelle)
            .containsExactly("COLLABORATEUR");
    }

    @Test
    void startupSeederRemovesCustomRolesAndTheirAssignments() {
        Role customRole = roleRepository.saveAndFlush(new Role("SPECIAL"));
        User customRoleUser = saveUser("special.test@psm.local", customRole);

        dataSeeder.run();

        assertThat(roleRepository.findByLibelle("SPECIAL")).isEmpty();
        assertThat(userRepository.findById(customRoleUser.getId()).orElseThrow().getRoles()).isEmpty();
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("StrongPass123!"));
        user.setTel("+21600000000");
        user.setNom("Test");
        user.setPrenom(role.getLibelle());
        user.setRoles(new HashSet<>(Set.of(role)));
        return userRepository.saveAndFlush(user);
    }

    private <T> ResponseEntity<T> exchange(
        String path,
        HttpMethod method,
        Object body,
        String token,
        Class<T> responseType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return http.exchange(path, method, new HttpEntity<>(body, headers), responseType);
    }
}
