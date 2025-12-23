package org.project. digital_logistics.integration.security;

import com.fasterxml. jackson.databind.ObjectMapper;
import org.junit.jupiter. api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org. junit.jupiter.api.Test;
import org.project.digital_logistics.dto.authJwt.LoginRequest;
import org. project.digital_logistics.model. User;
import org.project. digital_logistics.model.enums.Role;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework. beans.factory.annotation.Autowired;
import org.springframework. boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto. password.PasswordEncoder;
import org.springframework.test.context. ActiveProfiles;
import org. springframework.test.web.servlet. MockMvc;
import org. springframework.test.web.servlet. MvcResult;
import org. springframework.transaction.annotation.Transactional;

import static org. hamcrest.Matchers.*;
import static org.springframework. test.web.servlet.request. MockMvcRequestBuilders.*;
import static org.springframework.test. web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests - Authorization by Role")
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String warehouseManagerToken;
    private String clientToken;
    private String validPassword = "Password123!";

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Create ADMIN
        User admin = new User();
        admin.setName("Admin");
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(passwordEncoder. encode(validPassword));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        // Create WAREHOUSE_MANAGER
        User manager = new User();
        manager.setName("Manager");
        manager.setEmail("manager@test. com");
        manager.setPasswordHash(passwordEncoder.encode(validPassword));
        manager.setRole(Role.WAREHOUSE_MANAGER);
        manager.setActive(true);
        userRepository.save(manager);

        // Create CLIENT
        User client = new User();
        client.setName("Client");
        client.setEmail("client@test.com");
        client.setPasswordHash(passwordEncoder.encode(validPassword));
        client.setRole(Role.CLIENT);
        client.setActive(true);
        userRepository.save(client);

        // Login all users to get tokens
        adminToken = loginAndGetToken("admin@test.com");
        warehouseManagerToken = loginAndGetToken("manager@test.com");
        clientToken = loginAndGetToken("client@test.com");
    }

    private String loginAndGetToken(String email) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(validPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        return objectMapper.readTree(result. getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }


    @Test
    @DisplayName("CLIENT peut lire carriers - Doit retourner 200")
    void testClientCanReadCarriers() throws Exception {
        mockMvc.perform(get("/api/carriers")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CLIENT ne peut pas créer carrier - Doit retourner 403")
    void testClientCannotCreateCarrier() throws Exception {
        String carrierJson = "{\"name\":\"Test Carrier\",\"code\":\"TC001\",\"phoneNumber\":\"0612345678\"}";

        mockMvc.perform(post("/api/carriers")
                        .header("Authorization", "Bearer " + clientToken)
                        . contentType(MediaType.APPLICATION_JSON)
                        .content(carrierJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Accès refusé")));
    }

    @Test
    @DisplayName("ADMIN peut créer carrier - Doit retourner 201")
    void testAdminCanCreateCarrier() throws Exception {
        String carrierJson = "{\"name\": \"Test Carrier\",\"code\":\"TC001\",\"phoneNumber\":\"0612345678\",\"status\":\"AVAILABLE\"}";

        mockMvc. perform(post("/api/carriers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carrierJson))
                .andExpect(status().isCreated());
    }


    @Test
    @DisplayName("CLIENT peut lire produits actifs - Doit retourner 200")
    void testClientCanReadActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products/active")
                        . header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CLIENT ne peut pas lire tous les produits - Doit retourner 403")
    void testClientCannotReadAllProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    // ========== INVENTORY TESTS ==========

    @Test
    @DisplayName("CLIENT ne peut pas lire inventory - Doit retourner 403")
    void testClientCannotReadInventory() throws Exception {
        mockMvc. perform(get("/api/inventories")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT ne peut pas voir toutes les commandes - Doit retourner 403")
    void testClientCannotViewAllOrders() throws Exception {
        mockMvc.perform(get("/api/sales-orders")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN peut voir toutes les commandes - Doit retourner 200")
    void testAdminCanViewAllOrders() throws Exception {
        mockMvc.perform(get("/api/sales-orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("CLIENT ne peut pas lire users - Doit retourner 403")
    void testClientCannotReadUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN peut lire users - Doit retourner 200")
    void testAdminCanReadUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}