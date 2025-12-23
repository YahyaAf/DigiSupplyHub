package org.project.digital_logistics.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.digital_logistics.dto.authJwt.LoginRequest;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests - Authentication")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testAdmin;
    private User testClient;
    private String validPassword = "Password123!";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testAdmin = new User();
        testAdmin.setName("Admin Test");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPasswordHash(passwordEncoder.encode(validPassword));
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setActive(true);
        testAdmin = userRepository.save(testAdmin);

        testClient = new User();
        testClient.setName("Client Test");
        testClient.setEmail("client@test.com");
        testClient.setPasswordHash(passwordEncoder.encode(validPassword));
        testClient.setRole(Role.CLIENT);
        testClient.setActive(true);
        testClient = userRepository.save(testClient);
    }

    @Test
    @DisplayName("Login avec credentials valides - Doit retourner 200 et token")
    void testLoginWithValidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@test.com");
        loginRequest.setPassword(validPassword);

        mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Login avec email invalide - Doit retourner 401")
    void testLoginWithInvalidEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrong@test.com");
        loginRequest.setPassword(validPassword);

        mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("incorrect")));
    }

    @Test
    @DisplayName("Login avec mot de passe invalide - Doit retourner 401")
    void testLoginWithInvalidPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@test.com");
        loginRequest.setPassword("WrongPassword123!");

        mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("incorrect")));
    }

    @Test
    @DisplayName("Login avec compte désactivé - Doit retourner 400")
    void testLoginWithInactiveAccount() throws Exception {
        testAdmin.setActive(false);
        userRepository.save(testAdmin);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@test.com");
        loginRequest.setPassword(validPassword);

        mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()) // ← 400 au lieu de 401
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("disabled")));
    }

    @Test
    @DisplayName("Accès endpoint protégé SANS token - Doit retourner 401")
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/carriers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Non authentifié")))
                .andExpect(jsonPath("$.message", containsString("Token invalide")));
    }

    @Test
    @DisplayName("Accès endpoint protégé AVEC token valide - Doit retourner 200")
    void testAccessProtectedEndpointWithValidToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@test.com");
        loginRequest.setPassword(validPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Extraire le token de la réponse
        String response = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(response)
                .path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Accès avec token invalide - Doit retourner 401")
    void testAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout avec token valide - Doit retourner 200")
    void testLogoutWithValidToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@test.com");
        loginRequest.setPassword(validPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(response)
                .path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/auth/jwt/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}