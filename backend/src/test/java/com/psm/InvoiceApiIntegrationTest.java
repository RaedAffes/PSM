package com.psm;

import static org.assertj.core.api.Assertions.assertThat;

import com.psm.dto.InvoiceFileResponse;
import com.psm.dto.InvoiceResponse;
import com.psm.dto.InvoiceStats;
import com.psm.entity.Client;
import com.psm.entity.InvoiceStatus;
import com.psm.entity.Project;
import com.psm.entity.Role;
import com.psm.entity.User;
import com.psm.repository.ClientRepository;
import com.psm.repository.InvoiceRepository;
import com.psm.repository.ProjectRepository;
import com.psm.repository.RoleRepository;
import com.psm.repository.UserRepository;
import com.psm.service.EmailService;
import com.psm.service.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class InvoiceApiIntegrationTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EmailService emailService;

    private String adminToken;
    private String collaboratorToken;
    private Long clientId;
    private Long projectId;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        projectRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = roleRepository.save(new Role("ADMIN"));
        Role collaboratorRole = roleRepository.save(new Role("COLLABORATEUR"));
        User admin = saveUser("admin.invoice@psm.local", adminRole);
        User collaborator = saveUser("collab.invoice@psm.local", collaboratorRole);
        adminToken = jwtService.generateToken(admin);
        collaboratorToken = jwtService.generateToken(collaborator);

        Client client = new Client();
        client.setNom("Client Facture");
        client.setTel("+21600000000");
        client.setEmail("client.facture@psm.local");
        client.setAdresse("Tunis");
        client = clientRepository.saveAndFlush(client);
        clientId = client.getId();

        Project project = new Project();
        project.setLibelle("Projet Facture");
        project.setBudget(BigDecimal.valueOf(5000));
        project.setDuree(30);
        project.setDescription("Projet de test");
        project.setClient(client);
        project = projectRepository.saveAndFlush(project);
        projectId = project.getId();
    }

    @Test
    void invoiceLifecycleComputesTotalsAndEnforcesPermissions() {
        ResponseEntity<String> collaboratorList = exchange(
            "/api/invoices", HttpMethod.GET, null, collaboratorToken, String.class
        );
        assertThat(collaboratorList.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> collaboratorCreate = exchange(
            "/api/invoices", HttpMethod.POST, invoicePayload("FAC-COLLAB"), collaboratorToken, String.class
        );
        assertThat(collaboratorCreate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<InvoiceResponse> created = exchange(
            "/api/invoices", HttpMethod.POST, invoicePayload("FAC-2026-TEST"), adminToken, InvoiceResponse.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        InvoiceResponse invoice = created.getBody();
        assertThat(invoice).isNotNull();
        assertThat(invoice.numero()).isEqualTo("FAC-2026-TEST");
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(invoice.amount()).isEqualByComparingTo("1000");
        assertThat(invoice.taxRate()).isEqualByComparingTo("19");
        assertThat(invoice.totalAmount()).isEqualByComparingTo("1190.00");
        assertThat(invoice.clientId()).isEqualTo(clientId);
        assertThat(invoice.projectId()).isEqualTo(projectId);
        assertThat(invoice.dueDate()).isAfter(LocalDate.now());

        ResponseEntity<InvoiceFileResponse> uploadedFile = uploadFile(
            "/api/invoices/" + invoice.id() + "/files",
            "preuve facture".getBytes(StandardCharsets.UTF_8),
            "preuve.txt",
            collaboratorToken,
            InvoiceFileResponse.class
        );
        assertThat(uploadedFile.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(uploadedFile.getBody().fileName()).isEqualTo("preuve.txt");

        Long fileId = uploadedFile.getBody().id();
        ResponseEntity<String> collaboratorFileDelete = exchange(
            "/api/invoices/" + invoice.id() + "/files/" + fileId,
            HttpMethod.DELETE,
            null,
            collaboratorToken,
            String.class
        );
        assertThat(collaboratorFileDelete.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Void> adminFileDelete = exchange(
            "/api/invoices/" + invoice.id() + "/files/" + fileId,
            HttpMethod.DELETE,
            null,
            adminToken,
            Void.class
        );
        assertThat(adminFileDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> duplicate = exchange(
            "/api/invoices", HttpMethod.POST, invoicePayload("fac-2026-test"), adminToken, String.class
        );
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<InvoiceResponse[]> list = exchange(
            "/api/invoices", HttpMethod.GET, null, adminToken, InvoiceResponse[].class
        );
        assertThat(list.getBody()).hasSize(1);

        ResponseEntity<InvoiceResponse> paid = exchange(
            "/api/invoices/" + invoice.id() + "/paid?paymentMethod=Virement",
            HttpMethod.PATCH, null, adminToken, InvoiceResponse.class
        );
        assertThat(paid.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(paid.getBody().status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getBody().paidDate()).isEqualTo(LocalDate.now());

        ResponseEntity<InvoiceStats> paidStats = exchange(
            "/api/invoices/stats", HttpMethod.GET, null, adminToken, InvoiceStats.class
        );
        assertThat(paidStats.getBody().totalPaid()).isEqualByComparingTo("1190.00");
        assertThat(paidStats.getBody().totalUnpaid()).isEqualByComparingTo("0");
        assertThat(paidStats.getBody().paidCount()).isEqualTo(1);

        ResponseEntity<InvoiceResponse> unpaid = exchange(
            "/api/invoices/" + invoice.id() + "/unpaid",
            HttpMethod.PATCH, null, adminToken, InvoiceResponse.class
        );
        assertThat(unpaid.getBody().status()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(unpaid.getBody().paidDate()).isNull();

        ResponseEntity<Void> deleted = exchange(
            "/api/invoices/" + invoice.id(), HttpMethod.DELETE, null, adminToken, Void.class
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(invoiceRepository.count()).isZero();
    }

    private Map<String, Object> invoicePayload(String numero) {
        return Map.of(
            "numero", numero,
            "clientId", clientId,
            "projectId", projectId,
            "issueDate", LocalDate.now().toString(),
            "dueDate", LocalDate.now().plusDays(30).toString(),
            "amount", 1000,
            "taxRate", 19
        );
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

    private <T> ResponseEntity<T> uploadFile(String path, byte[] content, String filename, String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }
}
