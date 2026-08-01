package com.vpvpteam.xmlinvoicevalidationbackend;

import com.jayway.jsonpath.JsonPath;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.entity.UserEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.enums.Role;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final String TEST_USER_EMAIL = "user@integration-test.local";
    private static final String TEST_USER_PASSWORD = "user12345";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withInitScript("schema.sql");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    protected String adminEmail;

    @Value("${app.admin.password}")
    protected String adminPassword;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    validation_issue,
                    validation_output,
                    batch_invoice,
                    batch_vendor,
                    validation_batch,
                    business_rule,
                    vendor
                RESTART IDENTITY CASCADE
                """);
    }

    protected String adminToken() throws Exception {
        return tokenFor(adminEmail, adminPassword);
    }

    protected String userToken() throws Exception {
        if (!userRepository.existsByEmail(TEST_USER_EMAIL)) {
            UserEntity user = new UserEntity();
            user.setEmail(TEST_USER_EMAIL);
            user.setPasswordHash(passwordEncoder.encode(TEST_USER_PASSWORD));
            user.setRole(Role.USER);
            user.setActive(true);

            userRepository.save(user);
        }

        return tokenFor(TEST_USER_EMAIL, TEST_USER_PASSWORD);
    }

    private String tokenFor(String email, String password) throws Exception {
        String body = """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.token");
    }
}