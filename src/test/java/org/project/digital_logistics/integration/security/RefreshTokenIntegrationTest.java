package org.project.digital_logistics. integration.security;

import com. fasterxml.jackson.databind.ObjectMapper;
import org. junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter. api.Test;
import org.project.digital_logistics.dto.authJwt.RefreshTokenRequest;
import org.project.digital_logistics. model.RefreshToken;
import org.project.digital_logistics. model.User;
import org. project.digital_logistics.model. enums.Role;
import org.project.digital_logistics.repository.RefreshTokenRepository;
import org.project.digital_logistics. repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test. context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("user@test.com");
        testUser.setPasswordHash(passwordEncoder.encode(validPassword));
        testUser.setRole(Role. ADMIN);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Refresh avec token invalide - Doit retourner 400")
    void testRefreshWithInvalidToken() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");

        mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        . content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("invalide")));
    }

    @Test
    @DisplayName("Refresh avec token expiré - Doit retourner 400")
    void testRefreshWithExpiredToken() throws Exception {
        RefreshToken expiredToken = new RefreshToken();
        expiredToken. setToken("expired-token-123");
        expiredToken. setUser(testUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusDays(1));
        expiredToken. setRevoked(false);
        refreshTokenRepository.save(expiredToken);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("expired-token-123");

        mockMvc.perform(post("/api/auth/jwt/refresh")
                        .contentType(MediaType. APPLICATION_JSON)
                        . content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("expiré")));
    }
}