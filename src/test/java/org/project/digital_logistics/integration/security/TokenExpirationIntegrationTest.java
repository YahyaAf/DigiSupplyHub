package org.project.digital_logistics.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.digital_logistics.config.JwtUtil;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests - Token Expiration")
class TokenExpirationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String validPassword = "Password123!";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("user@test.com");
        testUser.setPasswordHash(passwordEncoder.encode(validPassword));
        testUser.setRole(Role.CLIENT);
        testUser.setActive(true);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Access token expiré - Doit retourner 401")
    void testExpiredAccessToken() throws Exception {
        String expiredToken = createExpiredToken();

        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token avec expiration dans le passé - Doit retourner 401")
    void testTokenWithPastExpiration() throws Exception {
        String pastToken = createTokenWithCustomExpiration(-3600000L); // -1 heure

        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer " + pastToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token sans claim exp - Doit retourner 401")
    void testTokenWithoutExpirationClaim() throws Exception {
        String tokenWithoutExp = createTokenWithoutExpiration();

        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer " + tokenWithoutExp))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token avec iat dans le futur - Doit retourner 401")
    void testTokenWithFutureIssuedAt() throws Exception {
        // Token avec iat dans le futur (attaque potentielle)
        String futureToken = createTokenWithFutureIssuedAt();

        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer " + futureToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token mal formé - Doit retourner 401")
    void testMalformedToken() throws Exception {
        String malformedToken = "not.a.valid.jwt.token";

        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token avec nbf (not before) dans le futur - Doit retourner 401")
    void testTokenWithFutureNotBefore() throws Exception {
        String futureNbfToken = createTokenWithFutureNotBefore();

        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer " + futureNbfToken))
                .andExpect(status().isUnauthorized());
    }

    private String createExpiredToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", testUser.getEmail());
        claims.put("role", testUser.getRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
                .setExpiration(new Date(System.currentTimeMillis() - 5000))
                .compact();
    }

    private String createTokenWithCustomExpiration(long millisFromNow) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", testUser.getEmail());
        claims.put("role", testUser.getRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + millisFromNow))
                .compact();
    }

    private String createTokenWithoutExpiration() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", testUser.getEmail());
        claims.put("role", testUser.getRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .compact();
    }

    private String createTokenWithFutureIssuedAt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", testUser.getEmail());
        claims.put("role", testUser.getRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis() + 3600000))
                .setExpiration(new Date(System.currentTimeMillis() + 7200000))
                .compact();
    }

    private String createTokenWithFutureNotBefore() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", testUser.getEmail());
        claims.put("role", testUser.getRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setNotBefore(new Date(System.currentTimeMillis() + 3600000))
                .setExpiration(new Date(System.currentTimeMillis() + 7200000))
                .compact();
    }

    private String getValidToken() throws Exception {
        String email = testUser.getEmail();
        String password = validPassword;

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", email);
        claims.put("role", testUser.getRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 heure
                .compact();
    }
}