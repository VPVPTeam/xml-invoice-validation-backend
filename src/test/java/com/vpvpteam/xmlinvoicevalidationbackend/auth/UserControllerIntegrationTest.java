package com.vpvpteam.xmlinvoicevalidationbackend.auth;

import com.vpvpteam.xmlinvoicevalidationbackend.AbstractIntegrationTest;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS_URL = "/api/users";
    private static final String NEW_USER_EMAIL = "new-user@integration-test.local";
    private static final String NEW_USER_PASSWORD = "newuser12345";

    @Test
    void createUser_asAdmin_returnsCreatedUserWithoutPassword() throws Exception {
        createUser(adminToken(), NEW_USER_EMAIL, NEW_USER_PASSWORD, Role.USER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(NEW_USER_EMAIL))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void createUser_thenCreatedUserCanLogIn() throws Exception {
        createUser(adminToken(), NEW_USER_EMAIL, NEW_USER_PASSWORD, Role.USER)
                .andExpect(status().isCreated());

        String token = tokenFor(NEW_USER_EMAIL, NEW_USER_PASSWORD);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(NEW_USER_EMAIL))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void createUser_withExistingEmail_returnsConflict() throws Exception {
        createUser(adminToken(), NEW_USER_EMAIL, NEW_USER_PASSWORD, Role.USER)
                .andExpect(status().isCreated());

        createUser(adminToken(), NEW_USER_EMAIL, NEW_USER_PASSWORD, Role.USER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists: " + NEW_USER_EMAIL));
    }

    @Test
    void createUser_asUser_returnsForbidden() throws Exception {
        createUser(userToken(), NEW_USER_EMAIL, NEW_USER_PASSWORD, Role.USER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void createUser_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(USERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(NEW_USER_EMAIL, NEW_USER_PASSWORD, Role.USER)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_withMalformedEmail_returnsBadRequest() throws Exception {
        createUser(adminToken(), "not-an-email", NEW_USER_PASSWORD, Role.USER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: Email must be a valid address"));
    }

    @Test
    void createUser_withTooShortPassword_returnsBadRequest() throws Exception {
        createUser(adminToken(), NEW_USER_EMAIL, "short", Role.USER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: Password must be between 8 and 100 characters"));
    }

    private ResultActions createUser(String token, String email, String password, Role role) throws Exception {
        return mockMvc.perform(post(USERS_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson(email, password, role)));
    }

    private String userJson(String email, String password, Role role) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "role": "%s"
                }
                """.formatted(email, password, role.name());
    }
}