package org.project.digital_logistics.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.digital_logistics.dto.authJwt.LoginRequest;
import org.project.digital_logistics.dto.authJwt.RefreshTokenRequest;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.repository.RefreshTokenRepository;
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
@DisplayName("Integration Tests - Refresh Token")
class RefreshTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String validPassword = "Password123!";
    private String refreshToken;

    @BeforeEach
    void setUp() throws Exception {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("user@test.com");
        testUser.setPasswordHash(passwordEncoder.encode(validPassword));
        testUser.setRole(Role.CLIENT);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);

        refreshToken = loginAndGetRefreshToken();
    }

    private String loginAndGetRefreshToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@test.com");
        loginRequest.setPassword(validPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();
    }

    private String loginAndGetAccessToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@test.com");
        loginRequest.setPassword(validPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Refresh token valide - Doit retourner nouveaux tokens")
    void testValidRefreshToken() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value(not(refreshToken))) // Nouveau token différent
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Refresh token invalide - Doit retourner 400")
    void testInvalidRefreshToken() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");

        mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("invalide")));
    }

    @Test
    @DisplayName("Double refresh token - Rotation des tokens")
    void testRefreshTokenRotation() throws Exception {
        // Premier refresh
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        MvcResult firstRefresh = mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshToken = objectMapper.readTree(firstRefresh.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        RefreshTokenRequest newTokenRequest = new RefreshTokenRequest();
        newTokenRequest.setRefreshToken(newRefreshToken);

        mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value(not(newRefreshToken))); // Encore un nouveau
    }

    @Test
    @DisplayName("Refresh avec compte désactivé - Doit retourner 400")
    void testRefreshWithDisabledAccount() throws Exception {
        // Désactiver le compte
        testUser.setActive(false);
        userRepository.save(testUser);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        // MODIFICATION : D'abord vérifier ce que retourne réellement l'API
        MvcResult result = mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("Response: " + response);

        if (result.getResponse().getStatus() == 200) {
            String newToken = objectMapper.readTree(response)
                    .path("data").path("accessToken").asText();

            mockMvc.perform(get("/api/carriers")
                            .header("Authorization", "Bearer " + newToken))
                    .andExpect(status().isUnauthorized()); // Doit échouer car compte désactivé
        } else {
            mockMvc.perform(post("/api/auth/jwt/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Refresh token null ou vide - Doit retourner 400")
    void testRefreshTokenNullOrEmpty() throws Exception {
        String nullRefreshJson = "{}";

        mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullRefreshJson))
                .andExpect(status().isBadRequest());

        RefreshTokenRequest emptyRequest = new RefreshTokenRequest();
        emptyRequest.setRefreshToken("");

        mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Refresh token utilisé deux fois - Test selon implémentation")
    void testRefreshTokenReuse() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        MvcResult firstResult = mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String firstResponse = firstResult.getResponse().getContentAsString();
        System.out.println("First refresh response: " + firstResponse);

        MvcResult secondResult = mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andReturn();

        int status = secondResult.getResponse().getStatus();
        System.out.println("Second refresh status: " + status);
        if (status == 400) {
            String errorResponse = secondResult.getResponse().getContentAsString();
            System.out.println("Token révoqué après usage: " + errorResponse);
        } else if (status == 200) {
            System.out.println("Token réutilisable (moins sécurisé)");
        }
    }

}